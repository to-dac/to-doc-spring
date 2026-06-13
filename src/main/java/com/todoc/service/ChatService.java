package com.todoc.service;

import com.todoc.domain.ChatMessage;
import com.todoc.domain.ChatSession;
import com.todoc.domain.User;
import com.todoc.domain.enums.MessageRole;
import com.todoc.domain.enums.MessageStatus;
import com.todoc.dto.request.CreateChatSessionRequest;
import com.todoc.dto.request.SendChatMessageRequest;
import com.todoc.dto.response.ChatMessageResponse;
import com.todoc.dto.response.ChatSessionResponse;
import com.todoc.exception.NotFoundException;
import com.todoc.repository.ChatMessageRepository;
import com.todoc.repository.ChatSessionRepository;
import com.todoc.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;

    @Transactional
    public ChatSessionResponse createSession(CreateChatSessionRequest request) {
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new NotFoundException("유저를 찾을 수 없습니다: id=" + request.userId()));

        String title = "새 채팅";
        if (request.address() != null && !request.address().trim().isEmpty()) {
            title = request.address().split("\\|")[0];
        }

        ChatSession session = ChatSession.builder()
                .user(user)
                .title(title)
                .address(request.address())
                .pnu(request.pnu())
                .templateId(request.templateId())
                .build();

        return ChatSessionResponse.from(chatSessionRepository.save(session));
    }

    @Transactional(readOnly = true)
    public List<ChatSessionResponse> getSessionsByUserId(Long userId) {
        return chatSessionRepository.findByUserIdOrderByUpdatedAtDesc(userId).stream()
                .map(ChatSessionResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getMessagesBySessionId(Long sessionId) {
        return chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId).stream()
                .map(ChatMessageResponse::from)
                .toList();
    }

    @Transactional
    public void deleteSession(Long sessionId) {
        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new NotFoundException("세션을 찾을 수 없습니다: id=" + sessionId));
        chatMessageRepository.deleteBySessionId(sessionId);
        chatSessionRepository.delete(session);
    }

    public SseEmitter sendStreamingMessage(Long sessionId, SendChatMessageRequest request) {
        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new NotFoundException("세션을 찾을 수 없습니다: id=" + sessionId));

        // 1. 사용자 메시지 DB에 즉시 저장
        saveMessage(session, MessageRole.USER, request.content());

        // 2. SseEmitter 생성 (타임아웃 3분)
        SseEmitter emitter = new SseEmitter(180000L);

        // 첫 연결 상태 전송
        try {
            emitter.send(SseEmitter.event()
                    .name("connect")
                    .data("연결 성공. AI 답변을 준비 중입니다."));
        } catch (IOException e) {
            log.error("SSE connection event error", e);
            emitter.completeWithError(e);
            return emitter;
        }

        // 3. Mock AI Response 생성 및 청크 분할
        String fullResponse = getMockResponse(request.content());
        // 줄바꿈이나 마침표 뒤 공백을 기준으로 쪼갬
        String[] chunks = fullResponse.split("(?<=\\n)|(?<=\\.\\s)");

        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        StringBuilder responseAccumulator = new StringBuilder();

        executor.scheduleAtFixedRate(new Runnable() {
            int index = 0;

            @Override
            public void run() {
                try {
                    if (index < chunks.length) {
                        String chunk = chunks[index];
                        responseAccumulator.append(chunk);

                        emitter.send(SseEmitter.event()
                                .name("chunk")
                                .data(chunk));
                        index++;
                    } else {
                        // 모든 청크 전송 완료
                        // AI 답변 DB 저장
                        saveMessage(session, MessageRole.ASSISTANT, responseAccumulator.toString());

                        // done 이벤트 전송
                        emitter.send(SseEmitter.event()
                                .name("done")
                                .data("답변 종료"));

                        emitter.complete();
                        executor.shutdown();
                    }
                } catch (Exception e) {
                    log.error("Error during streaming SSE response", e);
                    emitter.completeWithError(e);
                    executor.shutdown();
                }
            }
        }, 500, 400, TimeUnit.MILLISECONDS);

        // 스트리밍 강제 종료나 완료 시 리소스 정리
        emitter.onCompletion(executor::shutdown);
        emitter.onTimeout(executor::shutdown);
        emitter.onError(e -> executor.shutdown());

        return emitter;
    }

    @Transactional
    public void saveMessage(ChatSession session, MessageRole role, String content) {
        ChatMessage message = ChatMessage.builder()
                .session(session)
                .role(role)
                .content(content)
                .status(MessageStatus.COMPLETED)
                .build();
        chatMessageRepository.save(message);
    }

    private String getMockResponse(String prompt) {
        if (prompt.contains("편의시설") || prompt.contains("장애인") || prompt.contains("적합성") || prompt.contains("계획서")) {
            return "선택하신 성남시 분당구 부지는 제2종 일반주거지역으로 분류되어 있으며, 장애인·노인·임산부 등의 편의증진 보장에 관한 법률에 따라 장애인 편의시설 설치 계획서 작성이 필요합니다.\n\n" +
                    "1. 주출입구 접근로: 유효폭은 1.2m 이상이어야 하며, 기울기는 18분의 1 이하로 설계해야 합니다.\n" +
                    "2. 장애인 전용 주차구역: 크기는 폭 3.3m 이상, 길이 5m 이상이어야 하며, 주출입구와 가장 가까운 장소에 배치되어야 합니다.\n" +
                    "3. 높이 차이가 제거된 건축물 출입구: 장애인 등이 통행할 수 있는 출입구의 유효폭은 0.9m 이상이어야 합니다.\n\n" +
                    "해당 부지에 대한 적합성 분석을 완료했으며, 우측 화면에 '장애인 편의시설 설치 계획서' 초안 문서를 작성하여 띄워드렸습니다. 추가 질문이 있으시면 언제든지 편하게 물어보세요!";
        } else if (prompt.contains("소방") || prompt.contains("협의서") || prompt.contains("소방시설")) {
            return "해당 부지에 신축 예정인 제2종 근린생활시설의 경우 소방시설 설치 협의서 검토 대상에 해당합니다.\n\n" +
                    "1. 소화설비: 연면적 400㎡ 이상인 경우 기본 소화설비 외에 자동화재탐지설비가 요구됩니다.\n" +
                    "2. 피난구조설비: 유도등, 비상조명등 및 피난기구(완강기 등)의 규격 배치가 적절해야 소방협의가 승인됩니다.\n" +
                    "3. 경보설비: 비상방송설비의 음향 장치가 각 층마다 고르게 전달되도록 설계 기준을 맞추어야 합니다.\n\n" +
                    "이와 관련된 상세 소방 설계 가이드라인을 토대로 우측에 '소방시설 설치 협의서'의 세부 도면 초안을 매칭해 드렸습니다. 에디터 창에서 직접 수치를 조정하실 수 있습니다.";
        } else if (prompt.contains("용도변경") || prompt.contains("근생") || prompt.contains("근린생활시설")) {
            return "제2종 근린생활시설로의 용도변경은 국토교통부령 및 지자체 건축 조례의 용도변경 허가 기준을 충족해야 합니다.\n\n" +
                    "1. 주차장 대수 산정: 시설 면적 134㎡당 1대의 주차대수가 요구되며, 기존 주차장 규격을 만족해야 합니다.\n" +
                    "2. 하수도 원인자부담금: 하루 오수 발생량이 일정 기준 이상 증가할 경우, 하수도법에 따른 부담금이 부과될 수 있습니다.\n" +
                    "3. 소방 및 정화조 용량: 변경 예정 업종의 정화조 처리 용량을 사전 체크해야 합니다.\n\n" +
                    "우측 에디터에 '제2종 근린생활시설 용도변경 신청서' 초안을 활성화해 드렸으니 참고하시길 바랍니다.";
        } else {
            return "안녕하세요! 토지 인허가 가이드 AI 도우미 '토닥'입니다.\n\n" +
                    "검토 중이신 부지 주소에 대해 규제 분석을 요청하시거나, 필요한 건축 서류(예: 장애인 편의시설 설치 계획서, 소방시설 설치 협의서, 용도변경 신청서 등)에 대해 물어보시면 상세히 진단해 드릴 수 있습니다.\n\n" +
                    "현재 어떤 점이 궁금하신가요?";
        }
    }
}
