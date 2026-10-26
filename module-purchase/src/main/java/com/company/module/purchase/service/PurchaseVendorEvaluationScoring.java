package com.company.module.purchase.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class PurchaseVendorEvaluationScoring {

    private static final List<Criterion> CRITERIA = List.of(
        criterion("creditRating", "신용평가", "신용평가등급", "30",
            option("BB0_OR_HIGHER", "BB0 이상", "30"),
            option("BB_MINUS", "BB-", "28"),
            option("B_PLUS_TO_B_MINUS", "B+, B0, B-", "26"),
            option("CCC_PLUS_OR_LOWER_OR_MISSING", "CCC+ 이하 / 미제출", "24")),
        criterion("qualityCertification", "품질보증 신뢰도", "품질인증", "5",
            option("INTERNATIONAL_ISO", "국제(ISO)", "5"),
            option("DOMESTIC_KS", "국내(KS)", "4.5"),
            option("OTHER", "기타", "4"),
            option("NONE_OR_MISSING", "없음 / 미제출", "3")),
        criterion("qualityContribution", "품질보증 신뢰도", "품질 기여도", "5",
            option("QUALITY_IMPROVEMENT", "품질개선 기여", "5"),
            option("QUALITY_MAINTAINED", "품질동등 유지", "4.5"),
            option("PARTIAL_QUALITY_DECLINE", "품질 일부 저하", "3.5"),
            option("QUALITY_ISSUE", "품질문제 발생", "2")),
        criterion("industryExperience", "전문성", "동업계 경력", "5",
            option("SEVEN_YEARS_OR_MORE", "7년 이상", "5"),
            option("FIVE_YEARS_OR_MORE", "5년 이상", "4.5"),
            option("THREE_YEARS_OR_MORE", "3년 이상", "4"),
            option("UNDER_THREE_YEARS", "3년 미만", "3")),
        criterion("otherCustomerCount", "전문성", "타사 거래실적", "5",
            option("FIVE_COMPANIES_OR_MORE", "5개사 이상", "5"),
            option("THREE_COMPANIES_OR_MORE", "3개사 이상", "4.5"),
            option("TWO_COMPANIES", "2개사", "4"),
            option("UNDER_TWO_OR_NO_REPLY", "2개사 미만 / 미회신", "3")),
        criterion("productQuality", "제품 품질", "품질(사양, 규격 만족도)", "5",
            option("EXCELLENT", "탁월", "5"),
            option("GOOD", "우수", "4.5"),
            option("AVERAGE", "보통", "3.5"),
            option("POOR", "미흡", "2")),
        criterion("performance", "제품 품질", "성능", "5",
            option("EXCELLENT", "탁월", "5"),
            option("GOOD", "우수", "4.5"),
            option("AVERAGE", "보통", "3.5"),
            option("POOR", "미흡", "2")),
        criterion("defectCount", "제품 품질", "불량 발생 건수", "10",
            option("ZERO", "0건", "10"),
            option("ONE_TO_TWO", "1~2건", "8.5"),
            option("THREE_TO_FOUR", "3~4건", "7"),
            option("FIVE_OR_MORE", "5건 이상", "5")),
        criterion("ownProductionRate", "납기 준수", "자체 생산", "5",
            option("ONE_HUNDRED_PERCENT_OR_MORE", "100% 이상", "5"),
            option("EIGHTY_PERCENT_OR_MORE", "80% 이상", "4.5"),
            option("FIFTY_PERCENT_OR_MORE", "50% 이상", "4"),
            option("UNDER_FIFTY_OR_NO_REPLY", "50% 미만 / 미회신", "3"),
            option("NOT_APPLICABLE", "비대상", "4")),
        criterion("deliveryCompliance", "납기 준수", "납기", "10",
            option("PERFECT_ON_TIME", "100% 준수", "10"),
            option("ONE_TO_TWO_MISSES", "미준수 연 1~2회", "8.5"),
            option("THREE_MISSES", "미준수 연 3회", "7"),
            option("FOUR_OR_MORE_MISSES", "미준수 연 4회 이상", "5")),
        criterion("afterService", "협력도", "사후 관리", "5",
            option("EXCELLENT", "탁월", "5"),
            option("GOOD", "우수", "4.5"),
            option("AVERAGE", "보통", "3.5"),
            option("POOR", "미흡", "2")),
        criterion("workCooperation", "협력도", "업무협조( Trouble 발생시 협조도)", "10",
            option("EXCELLENT", "탁월", "10"),
            option("GOOD", "우수", "9"),
            option("AVERAGE", "보통", "7"),
            option("POOR", "미흡", "5"))
    );

    private PurchaseVendorEvaluationScoring() { }

    public static List<Criterion> criteria() {
        return CRITERIA;
    }

    public static Calculation calculate(Map<String, String> answers) {
        if (answers == null) throw new IllegalArgumentException("평가 항목을 모두 입력해 주세요.");
        Set<String> requiredCodes = CRITERIA.stream().map(Criterion::code).collect(Collectors.toSet());
        if (!answers.keySet().equals(requiredCodes)) {
            throw new IllegalArgumentException("평가 항목을 모두 선택해 주세요.");
        }

        List<ScoredCriterion> scores = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (Criterion criterion : CRITERIA) {
            String choiceCode = answers.get(criterion.code());
            Option option = criterion.options().stream()
                .filter(candidate -> candidate.code().equals(choiceCode))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(criterion.name() + " 선택값이 올바르지 않습니다."));
            scores.add(new ScoredCriterion(criterion.code(), option.code(), option.label(), option.score()));
            total = total.add(option.score());
        }
        return new Calculation(List.copyOf(scores), total, grade(total));
    }

    public static String grade(BigDecimal total) {
        if (total.compareTo(BigDecimal.valueOf(90)) >= 0) return "A";
        if (total.compareTo(BigDecimal.valueOf(80)) >= 0) return "B";
        if (total.compareTo(BigDecimal.valueOf(70)) >= 0) return "C";
        return "D";
    }

    private static Criterion criterion(String code, String section, String name, String maximum, Option... options) {
        return new Criterion(code, section, name, new BigDecimal(maximum), List.of(options));
    }

    private static Option option(String code, String label, String score) {
        return new Option(code, label, new BigDecimal(score));
    }

    public record Criterion(String code, String section, String name, BigDecimal maximumScore, List<Option> options) { }
    public record Option(String code, String label, BigDecimal score) { }
    public record ScoredCriterion(String code, String optionCode, String optionLabel, BigDecimal score) { }
    public record Calculation(List<ScoredCriterion> criteria, BigDecimal totalScore, String grade) { }
}
