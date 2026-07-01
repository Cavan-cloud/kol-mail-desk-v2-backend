package com.lovart.maildesk.common.feishu;

import com.lovart.maildesk.common.enums.KolStage;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class FeishuStageMapperTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "已询价", "询价", "追+", "二追", "追加", "其他追问", "  已询价  "
    })
    void outreachStage(String feishuStatus) {
        assertThat(FeishuStageMapper.mapFeishuStage(feishuStatus)).isEqualTo(KolStage.OUTREACH);
    }

    @ParameterizedTest
    @ValueSource(strings = {"议价中", "议价"})
    void negotiatingStage(String feishuStatus) {
        assertThat(FeishuStageMapper.mapFeishuStage(feishuStatus)).isEqualTo(KolStage.NEGOTIATING);
    }

    @ParameterizedTest
    @ValueSource(strings = {"价格确定待合作", "已合作待签合同", "已合作"})
    void confirmedStage(String feishuStatus) {
        assertThat(FeishuStageMapper.mapFeishuStage(feishuStatus)).isEqualTo(KolStage.CONFIRMED);
    }

    @ParameterizedTest
    @ValueSource(strings = {"待脚本", "脚本修改中", "待初稿", "修改视频中"})
    void producingStage(String feishuStatus) {
        assertThat(FeishuStageMapper.mapFeishuStage(feishuStatus)).isEqualTo(KolStage.PRODUCING);
    }

    @ParameterizedTest
    @ValueSource(strings = {"已审核待发布", "待发布"})
    void reviewingStage(String feishuStatus) {
        assertThat(FeishuStageMapper.mapFeishuStage(feishuStatus)).isEqualTo(KolStage.REVIEWING);
    }

    @ParameterizedTest
    @ValueSource(strings = {"已发布待付款"})
    void publishedStage(String feishuStatus) {
        assertThat(FeishuStageMapper.mapFeishuStage(feishuStatus)).isEqualTo(KolStage.PUBLISHED);
    }

    @ParameterizedTest
    @ValueSource(strings = {"已付款"})
    void payingStage(String feishuStatus) {
        assertThat(FeishuStageMapper.mapFeishuStage(feishuStatus)).isEqualTo(KolStage.PAYING);
    }

    @ParameterizedTest
    @ValueSource(strings = {"合作过"})
    void reinvestStage(String feishuStatus) {
        assertThat(FeishuStageMapper.mapFeishuStage(feishuStatus)).isEqualTo(KolStage.REINVEST);
    }

    @ParameterizedTest
    @ValueSource(strings = {"已拒绝", "剔除合作", "放弃合作"})
    void declinedStage(String feishuStatus) {
        assertThat(FeishuStageMapper.mapFeishuStage(feishuStatus)).isEqualTo(KolStage.DECLINED);
    }

    @ParameterizedTest
    @ValueSource(strings = {"未合作过", "移至3月", "转4月"})
    void administrativeMarkersPreserveExistingStage(String feishuStatus) {
        assertThat(FeishuStageMapper.mapFeishuStage(feishuStatus)).isNull();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "未知状态XYZ"})
    void unknownOrEmptyReturnsNull(String feishuStatus) {
        assertThat(FeishuStageMapper.mapFeishuStage(feishuStatus)).isNull();
    }

    @ParameterizedTest
    @MethodSource("priorityCases")
    void moreSpecificRulesWin(String feishuStatus, KolStage expected) {
        assertThat(FeishuStageMapper.mapFeishuStage(feishuStatus)).isEqualTo(expected);
    }

    static Stream<Arguments> priorityCases() {
        return Stream.of(
                Arguments.of("已发布待付款", KolStage.PUBLISHED),
                Arguments.of("已付款", KolStage.PAYING),
                Arguments.of("已合作待签合同", KolStage.CONFIRMED),
                Arguments.of("合作过", KolStage.REINVEST));
    }
}
