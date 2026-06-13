// 토지 정보 조회 응답 DTO
package com.todoc.dto.response;

import com.todoc.domain.FormTemplate;
import com.todoc.dto.external.BuildingInfoResponse;
import com.todoc.dto.external.VWorldLandResponse;
import com.todoc.dto.external.VWorldLandUseResponse;

import java.util.List;

public record LandInfoResponse(
        String pnu,
        String address,
        String ldCodeNm,
        String lndcgrCodeNm,
        String lndpclAr,
        String oficlLndpcl,
        String prposArea1Nm,
        String prposArea2Nm,
        String ladUseSittnNm,
        String tpgrphHgCodeNm,
        String tpgrphFrmCodeNm,
        String roadSideCodeNm,
        String pblntfPclnd,
        String lastUpdtDt,
        Building building,
        List<LandUse> landUses,
        List<PermitTemplate> applicablePermits
) {

    public record LandUse(
            String code,
            String name,
            String conflictType
    ) {
        public static LandUse from(VWorldLandUseResponse.Field field) {
            return new LandUse(field.code(), field.name(), field.conflictType());
        }
    }

    public record PermitTemplate(
            Long id,
            String templateCode,
            String name,
            String description
    ) {
        public static PermitTemplate from(FormTemplate template) {
            return new PermitTemplate(
                    template.getId(),
                    template.getTemplateCode(),
                    template.getName(),
                    template.getDescription());
        }
    }

    public record Building(
            boolean hasBuilding,
            String bldNm,
            String mainPurpsCdNm,
            String etcPurps,
            Double platArea,
            Double archArea,
            Double totArea,
            Double bcRat,
            Double vlRat,
            Double heit,
            Integer grndFlrCnt,
            Integer ugrndFlrCnt,
            String useAprDay,
            String strctCdNm
    ) {}

    public static LandInfoResponse of(String pnu, String address,
            VWorldLandResponse.Field field, BuildingInfoResponse bldg,
            List<VWorldLandUseResponse.Field> uses, List<FormTemplate> applicablePermits) {
        Building building = bldg == null
                ? new Building(false, null, null, null, null, null, null, null, null, null, null, null, null, null)
                : new Building(true,
                        bldg.bldNm(), bldg.mainPurpsCdNm(), bldg.etcPurps(),
                        bldg.platArea(), bldg.archArea(), bldg.totArea(),
                        bldg.bcRat(), bldg.vlRat(), bldg.heit(),
                        bldg.grndFlrCnt(), bldg.ugrndFlrCnt(),
                        bldg.useAprDay(), bldg.strctCdNm());

        List<LandUse> landUses = uses.stream().map(LandUse::from).toList();
        List<PermitTemplate> permits = applicablePermits.stream().map(PermitTemplate::from).toList();

        return new LandInfoResponse(
                pnu, address,
                field.ldCodeNm(), field.lndcgrCodeNm(), field.lndpclAr(), field.oficlLndpcl(),
                field.prposArea1Nm(), field.prposArea2Nm(), field.ladUseSittnNm(),
                field.tpgrphHgCodeNm(), field.tpgrphFrmCodeNm(), field.roadSideCodeNm(),
                field.pblntfPclnd(), field.lastUpdtDt(),
                building, landUses, permits);
    }
}
