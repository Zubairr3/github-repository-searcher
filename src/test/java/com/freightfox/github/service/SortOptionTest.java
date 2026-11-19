package com.freightfox.github.service;

import com.freightfox.github.api.BadRequestException;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class SortOptionTest {
    @Test void defaultSortIsStars() {
        assertThat(SortOption.parse(null)).isEqualTo(SortOption.STARS);
        assertThat(SortOption.parse("  ")).isEqualTo(SortOption.STARS);
    }
    @Test void acceptsOtherSortsIgnoringCase() {
        assertThat(SortOption.parse("FoRkS")).isEqualTo(SortOption.FORKS);
        assertThat(SortOption.parse("updated")).isEqualTo(SortOption.UPDATED);
    }
    @Test void rejectsUnsupportedSort() {
        assertThatThrownBy(() -> SortOption.parse("trending"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("sort must be");
    }
}
