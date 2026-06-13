// 토지 정보 조회 REST API 엔드포인트
package com.todoc.controller;

import com.todoc.dto.response.LandInfoResponse;
import com.todoc.service.LandService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Land", description = "토지 정보 조회")
@RestController
@RequestMapping("/api/land")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class LandController {

    private final LandService landService;

    @Operation(summary = "토지 정보 조회", description = "위도·경도로 PNU를 조회하고 토지특성·이용규제·건물 정보를 반환합니다.")
    @GetMapping
    public LandInfoResponse getLandInfo(
            @Parameter(description = "위도 (예: 37.50084)", required = true) @RequestParam double lat,
            @Parameter(description = "경도 (예: 127.03674)", required = true) @RequestParam double lng) {
        return landService.getLandInfo(lat, lng);
    }
}
