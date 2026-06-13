// 토지 정보 조회 비즈니스 로직 — VWorld WFS 좌표 → PNU → 토지특성·규제·건물 조회
package com.todoc.service;

import com.todoc.client.BuildingRegisterClient;
import com.todoc.client.VWorldClient;
import com.todoc.domain.FormTemplate;
import com.todoc.dto.external.BuildingInfoResponse;
import com.todoc.dto.external.VWorldLandResponse;
import com.todoc.dto.external.VWorldLandUseResponse;
import com.todoc.dto.external.VWorldWfsResponse;
import com.todoc.dto.response.LandInfoResponse;
import com.todoc.repository.FormTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class LandService {

    private static final Set<String> FARMLAND_CATEGORIES = Set.of("전", "답", "과수원", "목장용지", "농지");
    private static final Set<String> MOUNTAIN_CATEGORIES = Set.of("임야");

    private final VWorldClient vWorldClient;
    private final BuildingRegisterClient buildingRegisterClient;
    private final FormTemplateRepository formTemplateRepository;

    @Transactional(readOnly = true)
    public LandInfoResponse getLandInfo(double lat, double lng) {
        VWorldWfsResponse.Properties wfs = vWorldClient.getPnuByCoordinate(lat, lng);
        String pnu = wfs.pnu();
        String address = wfs.addr();
        VWorldLandResponse.Field field = vWorldClient.getLandCharacteristics(pnu);
        BuildingInfoResponse building = buildingRegisterClient.getBuilding(pnu);
        List<VWorldLandUseResponse.Field> landUses = vWorldClient.getLandUses(pnu);
        List<String> permitCodes = resolvePermitCodes(field, landUses);
        List<FormTemplate> permits = formTemplateRepository.findAllByActiveTrueAndTemplateCodeIn(permitCodes);
        return LandInfoResponse.of(pnu, address, field, building, landUses, permits);
    }

    private List<String> resolvePermitCodes(VWorldLandResponse.Field field,
            List<VWorldLandUseResponse.Field> landUses) {
        List<String> codes = new ArrayList<>();
        String category = field.lndcgrCodeNm();  // 지목명
        String useZone = field.prposArea1Nm();    // 용도지역명

        boolean isUrban = useZone != null && useZone.contains("지역");
        boolean isFarmland = category != null && FARMLAND_CATEGORIES.stream()
                .anyMatch(category::contains);
        boolean isMountain = category != null && MOUNTAIN_CATEGORIES.stream()
                .anyMatch(category::contains);
        boolean hasRiverRegulation = landUses.stream()
                .anyMatch(u -> u.name() != null && u.name().contains("하천"));

        // 도시지역 내 건축 가능한 지목(대)이면 건축허가 대상
        if (isUrban && category != null && category.contains("대")) {
            codes.add("building_major_repair_use_change_permit");
        }
        // 도시지역이면 개발행위허가 대상
        if (isUrban) {
            codes.add("development_activity_permit");
        }
        // 농지이면 농지전용허가 대상
        if (isFarmland) {
            codes.add("farmland_conversion_permit");
        }
        // 임야이면 산지전용허가 대상
        if (isMountain) {
            codes.add("mountain_permit");
        }
        // 도로점용허가는 항상 포함 (도로 사용이 수반되는 모든 개발행위에 해당)
        codes.add("road_permit");
        // 하천 관련 규제 구역이면 하천점용허가 대상
        if (hasRiverRegulation) {
            codes.add("river_permit");
        }

        return codes;
    }
}
