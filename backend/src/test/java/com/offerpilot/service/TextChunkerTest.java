package com.offerpilot.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TextChunkerTest {
    private final TextChunker chunker = new TextChunker();

    @Test
    void keepsChildrenLinkedToTheirParentSections() {
        String input = "# 第一章 缴费范围\n\n" + "缴费政策内容。".repeat(80)
                + "\n\n# 第二章 生效日期\n\n本办法自二〇二六年一月一日起施行。";

        var parents = chunker.splitHierarchical(input);

        assertThat(parents).hasSize(2);
        assertThat(parents.get(0).sectionTitle()).isEqualTo("第一章 缴费范围");
        assertThat(parents.get(0).children()).isNotEmpty();
        assertThat(parents.get(1).children().getFirst().content()).contains("施行");
        assertThat(parents.stream().flatMap(parent -> parent.children().stream()).map(TextChunker.Chunk::index))
                .doesNotHaveDuplicates();
    }

    @Test
    void limitsChildSizeAndCreatesOverlapForLongText() {
        var children = chunker.split("很长的政策条款。".repeat(160));

        assertThat(children).hasSizeGreaterThan(1);
        assertThat(children).allSatisfy(child -> assertThat(child.content().length()).isLessThanOrEqualTo(650));
    }
}
