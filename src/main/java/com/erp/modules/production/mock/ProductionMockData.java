package com.erp.modules.production.mock;

import com.erp.modules.production.dto.ProductDto;
import com.erp.modules.production.dto.ProductionLineDto;
import com.erp.modules.production.dto.WorkOrderDto;
import com.erp.modules.production.dto.WorkOrderMaterialRequirementDto;
import com.erp.modules.production.model.MaterialRequirementStatus;
import com.erp.modules.production.model.ProductionLineStatus;
import com.erp.modules.production.model.WorkOrderPriority;
import com.erp.modules.production.model.WorkOrderStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ProductionMockData {

    private ProductionMockData() {
    }

    public static List<ProductDto> products() {
        return List.of(
            ProductDto.builder().code("TRF-1600").name("1600 kVA Yağlı Tip Dağıtım Trafosu").productGroup("Güç Trafosu").standardProductionHours(BigDecimal.valueOf(96)).defaultUnit("Adet").bomAvailable(true).build(),
            ProductDto.builder().code("TRF-2500H").name("2500 kVA Hermetik Tip Trafo").productGroup("Güç Trafosu").standardProductionHours(BigDecimal.valueOf(124)).defaultUnit("Adet").bomAvailable(true).build(),
            ProductDto.builder().code("KTR-1000").name("1000 kVA Kuru Tip Trafo").productGroup("Kuru Tip Trafo").standardProductionHours(BigDecimal.valueOf(88)).defaultUnit("Adet").bomAvailable(true).build(),
            ProductDto.builder().code("OGH-KMP").name("Orta Gerilim Hücre Komponenti").productGroup("OG Komponent").standardProductionHours(BigDecimal.valueOf(24)).defaultUnit("Adet").bomAvailable(true).build(),
            ProductDto.builder().code("TNK-OZL-01").name("Özel Tasarım Trafo Tankı").productGroup("Tank İmalatı").standardProductionHours(BigDecimal.valueOf(42)).defaultUnit("Adet").bomAvailable(true).build()
        );
    }

    public static List<ProductionLineDto> productionLines() {
        return List.of(
            ProductionLineDto.builder().code("BLW-01").name("Bobinaj Hattı").capacity(BigDecimal.valueOf(6)).capacityUnit("adet/hafta").status(ProductionLineStatus.AVAILABLE).responsibleTeam("Balıkesir Bobinaj Ekibi").suitable(true).build(),
            ProductionLineDto.builder().code("MNT-01").name("Montaj Hattı").capacity(BigDecimal.valueOf(5)).capacityUnit("adet/hafta").status(ProductionLineStatus.BUSY).responsibleTeam("Montaj Operasyon Ekibi").suitable(true).build(),
            ProductionLineDto.builder().code("TNK-01").name("Tank İmalat Hattı").capacity(BigDecimal.valueOf(4)).capacityUnit("adet/hafta").status(ProductionLineStatus.AVAILABLE).responsibleTeam("Metal İşleme Ekibi").suitable(true).build(),
            ProductionLineDto.builder().code("FIR-01").name("Kurutma Fırını").capacity(BigDecimal.valueOf(8)).capacityUnit("adet/gün").status(ProductionLineStatus.MAINTENANCE).responsibleTeam("Bakım ve Enerji Ekibi").suitable(false).build(),
            ProductionLineDto.builder().code("LAB-01").name("Test Laboratuvarı").capacity(BigDecimal.valueOf(10)).capacityUnit("test/gün").status(ProductionLineStatus.AVAILABLE).responsibleTeam("Kalite Güvence Ekibi").suitable(true).build(),
            ProductionLineDto.builder().code("BOY-01").name("Boyahane").capacity(BigDecimal.valueOf(8)).capacityUnit("adet/gün").status(ProductionLineStatus.OFFLINE).responsibleTeam("Yüzey İşlem Ekibi").suitable(false).build()
        );
    }

    public static List<String> responsibleUsers() {
        return List.of(
            "Üretim Şefi",
            "Bobinaj Sorumlusu",
            "Montaj Sorumlusu",
            "Kalite Kontrol Sorumlusu",
            "Planlama Uzmanı"
        );
    }

    public static List<String> shifts() {
        return List.of(
            "Gündüz Vardiyası",
            "Gece Vardiyası",
            "Hafta Sonu Vardiyası"
        );
    }

    public static Map<String, List<WorkOrderMaterialRequirementDto>> bomTemplates() {
        Map<String, List<WorkOrderMaterialRequirementDto>> templates = new LinkedHashMap<>();
        templates.put("TRF-1600", List.of(
            template("MAT-CU-120", "Bakır Tel 1.20 mm", BigDecimal.valueOf(1180), BigDecimal.valueOf(2100), "kg"),
            template("MAT-SL-001", "Silisli Sac", BigDecimal.valueOf(2450), BigDecimal.valueOf(6200), "kg"),
            template("MAT-IZ-010", "İzolasyon Kağıdı", BigDecimal.valueOf(180), BigDecimal.valueOf(260), "kg"),
            template("MAT-OIL-01", "Trafo Yağı", BigDecimal.valueOf(860), BigDecimal.valueOf(600), "lt"),
            template("MAT-TRM-06", "Bağlantı Terminali", BigDecimal.valueOf(6), BigDecimal.valueOf(24), "adet"),
            template("MAT-STL-02", "Çelik Konstrüksiyon Parçası", BigDecimal.ONE, BigDecimal.valueOf(3), "set")
        ));
        templates.put("TRF-2500H", List.of(
            template("MAT-CU-120", "Bakır Tel 1.20 mm", BigDecimal.valueOf(1560), BigDecimal.valueOf(2100), "kg"),
            template("MAT-SL-001", "Silisli Sac", BigDecimal.valueOf(3120), BigDecimal.valueOf(6200), "kg"),
            template("MAT-IZ-010", "İzolasyon Kağıdı", BigDecimal.valueOf(230), BigDecimal.valueOf(260), "kg"),
            template("MAT-OIL-01", "Trafo Yağı", BigDecimal.valueOf(1120), BigDecimal.valueOf(600), "lt"),
            template("MAT-TRM-06", "Bağlantı Terminali", BigDecimal.valueOf(8), BigDecimal.valueOf(24), "adet"),
            template("MAT-BOX-01", "Ambalaj Paleti", BigDecimal.ONE, BigDecimal.valueOf(5), "adet")
        ));
        templates.put("KTR-1000", List.of(
            template("MAT-CU-120", "Bakır Tel 1.20 mm", BigDecimal.valueOf(920), BigDecimal.valueOf(2100), "kg"),
            template("MAT-EPX-08", "Epoksi Reçine", BigDecimal.valueOf(140), BigDecimal.valueOf(120), "kg"),
            template("MAT-IZ-010", "İzolasyon Kağıdı", BigDecimal.valueOf(95), BigDecimal.valueOf(260), "kg"),
            template("MAT-TRM-06", "Bağlantı Terminali", BigDecimal.valueOf(4), BigDecimal.valueOf(24), "adet")
        ));
        templates.put("OGH-KMP", List.of(
            template("MAT-STL-02", "Çelik Konstrüksiyon Parçası", BigDecimal.valueOf(2), BigDecimal.valueOf(3), "set"),
            template("MAT-TRM-06", "Bağlantı Terminali", BigDecimal.valueOf(12), BigDecimal.valueOf(24), "adet"),
            template("MAT-PNT-01", "Koruyucu Boya", BigDecimal.valueOf(8), BigDecimal.valueOf(32), "lt")
        ));
        templates.put("TNK-OZL-01", List.of(
            template("MAT-STL-02", "Çelik Konstrüksiyon Parçası", BigDecimal.valueOf(3), BigDecimal.valueOf(3), "set"),
            template("MAT-PNT-01", "Koruyucu Boya", BigDecimal.valueOf(12), BigDecimal.valueOf(32), "lt"),
            template("MAT-BOX-01", "Ambalaj Paleti", BigDecimal.ONE, BigDecimal.valueOf(5), "adet")
        ));
        return templates;
    }

    public static List<WorkOrderDto> workOrders() {
        LocalDate today = LocalDate.now();
        return List.of(
            WorkOrderDto.builder().workOrderNo("WO-2026-0001").product(product("TRF-1600", "1600 kVA Yağlı Tip Dağıtım Trafosu")).plannedQuantity(BigDecimal.valueOf(2)).producedQuantity(BigDecimal.ZERO).unit("Adet").productionLine(line("MNT-01", "Montaj Hattı")).priority(WorkOrderPriority.HIGH).status(WorkOrderStatus.PLANNED).plannedStartDate(today.plusDays(1)).plannedEndDate(today.plusDays(7)).responsible("Montaj Sorumlusu").shift("Gündüz Vardiyası").description("Balıkesir merkez dağıtım sahası için planlanan sipariş.").build(),
            WorkOrderDto.builder().workOrderNo("WO-2026-0002").product(product("TRF-2500H", "2500 kVA Hermetik Tip Trafo")).plannedQuantity(BigDecimal.ONE).producedQuantity(BigDecimal.valueOf(0.45)).unit("Adet").productionLine(line("BLW-01", "Bobinaj Hattı")).priority(WorkOrderPriority.CRITICAL).status(WorkOrderStatus.IN_PROGRESS).plannedStartDate(today.minusDays(3)).plannedEndDate(today.plusDays(4)).responsible("Üretim Şefi").shift("Gündüz Vardiyası").description("Bandırma OSB teslimatına yetişecek kritik iş emri.").build(),
            WorkOrderDto.builder().workOrderNo("WO-2026-0003").product(product("KTR-1000", "1000 kVA Kuru Tip Trafo")).plannedQuantity(BigDecimal.valueOf(3)).producedQuantity(BigDecimal.ONE).unit("Adet").productionLine(line("FIR-01", "Kurutma Fırını")).priority(WorkOrderPriority.HIGH).status(WorkOrderStatus.PAUSED).plannedStartDate(today.minusDays(5)).plannedEndDate(today.plusDays(2)).responsible("Bobinaj Sorumlusu").shift("Gece Vardiyası").description("Kurutma hattı bakım planı nedeniyle durduruldu.").build(),
            WorkOrderDto.builder().workOrderNo("WO-2026-0004").product(product("OGH-KMP", "Orta Gerilim Hücre Komponenti")).plannedQuantity(BigDecimal.valueOf(8)).producedQuantity(BigDecimal.valueOf(8)).unit("Adet").productionLine(line("LAB-01", "Test Laboratuvarı")).priority(WorkOrderPriority.NORMAL).status(WorkOrderStatus.COMPLETED).plannedStartDate(today.minusDays(10)).plannedEndDate(today.minusDays(2)).responsible("Kalite Kontrol Sorumlusu").shift("Gündüz Vardiyası").description("Rutin OG komponent üretim planı tamamlandı.").build(),
            WorkOrderDto.builder().workOrderNo("WO-2026-0005").product(product("TNK-OZL-01", "Özel Tasarım Trafo Tankı")).plannedQuantity(BigDecimal.valueOf(2)).producedQuantity(BigDecimal.ZERO).unit("Adet").productionLine(line("TNK-01", "Tank İmalat Hattı")).priority(WorkOrderPriority.NORMAL).status(WorkOrderStatus.DRAFT).plannedStartDate(today.plusDays(3)).plannedEndDate(today.plusDays(10)).responsible("Planlama Uzmanı").shift("Gündüz Vardiyası").description("Özel proje tank imalatı için taslak hazırlık kaydı.").build(),
            WorkOrderDto.builder().workOrderNo("WO-2026-0006").product(product("TRF-1600", "1600 kVA Yağlı Tip Dağıtım Trafosu")).plannedQuantity(BigDecimal.ONE).producedQuantity(BigDecimal.ZERO).unit("Adet").productionLine(line("BOY-01", "Boyahane")).priority(WorkOrderPriority.CRITICAL).status(WorkOrderStatus.DELAYED).plannedStartDate(today.minusDays(7)).plannedEndDate(today.minusDays(1)).responsible("Montaj Sorumlusu").shift("Hafta Sonu Vardiyası").description("Boyahane devre dışı olduğu için sevkiyat öncesi son operasyon gecikti.").build(),
            WorkOrderDto.builder().workOrderNo("WO-2026-0007").product(product("TRF-2500H", "2500 kVA Hermetik Tip Trafo")).plannedQuantity(BigDecimal.ONE).producedQuantity(BigDecimal.ZERO).unit("Adet").productionLine(line("MNT-01", "Montaj Hattı")).priority(WorkOrderPriority.LOW).status(WorkOrderStatus.CANCELLED).plannedStartDate(today.minusDays(2)).plannedEndDate(today.plusDays(3)).responsible("Planlama Uzmanı").shift("Gündüz Vardiyası").description("Müşteri revizyonu nedeniyle iptal edildi.").build(),
            WorkOrderDto.builder().workOrderNo("WO-2026-0008").product(product("KTR-1000", "1000 kVA Kuru Tip Trafo")).plannedQuantity(BigDecimal.ONE).producedQuantity(BigDecimal.ZERO).unit("Adet").productionLine(line("BLW-01", "Bobinaj Hattı")).priority(WorkOrderPriority.NORMAL).status(WorkOrderStatus.PLANNED).plannedStartDate(today).plannedEndDate(today.plusDays(5)).responsible("Bobinaj Sorumlusu").shift("Gündüz Vardiyası").description("Bugün devreye alınacak standart kuru tip üretim emri.").build()
        );
    }

    private static WorkOrderMaterialRequirementDto template(String code, String name, BigDecimal baseQuantity, BigDecimal availableStock, String unit) {
        return WorkOrderMaterialRequirementDto.builder()
            .materialCode(code)
            .materialName(name)
            .baseQuantity(baseQuantity)
            .requiredQuantity(baseQuantity)
            .availableStock(availableStock)
            .shortageQuantity(BigDecimal.ZERO)
            .unit(unit)
            .status(MaterialRequirementStatus.AVAILABLE)
            .build();
    }

    private static ProductDto product(String code, String name) {
        return ProductDto.builder().code(code).name(name).build();
    }

    private static ProductionLineDto line(String code, String name) {
        return ProductionLineDto.builder().code(code).name(name).build();
    }
}