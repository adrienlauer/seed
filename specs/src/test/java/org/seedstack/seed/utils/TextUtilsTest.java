/**
 * Copyright (c) 2013-2016, The SeedStack authors <http://seedstack.org>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.seedstack.seed.utils;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TextUtilsTest {
    private String longString = "Nec vox accusatoris ulla licet subditicii in his malorum quaerebatur acervis ut saltem specie tenus crimina praescriptis legum committerentur, quod aliquotiens fecere principes saevi: sed quicquid Caesaris implacabilitati sedisset, id velut fas iusque perpensum confestim urgebatur impleri.";
    private String longWord = "abcdefghijklmnopqrstuvwxyz----abcdefghijklmnopqrstuvwxyz----abcdefghijklmnopqrstuvwxyz----";

    @Test
    public void testSplitWords() throws Exception {
        String words[] = TextUtils.splitWords(longString);
        assertThat(String.join(" ", (CharSequence[]) words)).isEqualTo(longString);
    }

    @Test
    public void testSimpleWrap() throws Exception {
        String wrapped = TextUtils.wrap(longString, 80);
        for (String line : wrapped.split("\n")) {
            assertThat(line.length()).isLessThanOrEqualTo(80);
        }
    }

    @Test
    public void testLongWordWrap() throws Exception {
        String wrapped = TextUtils.wrap(longWord, 80);
        for (String line : wrapped.split("\n")) {
            assertThat(line.length()).isLessThanOrEqualTo(80);
        }
        assertThat(wrapped).contains("—");
        System.out.println(wrapped);
    }
}