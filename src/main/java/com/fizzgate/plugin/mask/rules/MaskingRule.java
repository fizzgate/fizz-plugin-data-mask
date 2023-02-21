/*
 *  Copyright (C) 2020 the original author or authors.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.fizzgate.plugin.mask.rules;

/**
 * 掩码规则接口
 */
public interface MaskingRule {
    String EMPTY_STR = "";
    String SPACE_STR = " ";
    char CHAR_STAR = '*';

    String DEFAULT_STR = "default";
    String IDCARD_STR = "idcard";
    String NAME_STR = "name";
    String PHONE_STR = "phone";
    String BLANK_STR = "blank";
    /**
     * 默认处理
     * @param val
     * @return
     */
    String defaultOut(String val);

    /**
     * 证件信息掩码处理，身份证等
     * @param val
     * @return
     */
    String idCard(String val);

    /**
     * 姓名掩码处理
     * @param val
     * @return
     */
    String name(String val);

    /**
     * 电话号码，手机号码掩码处理
     * @param val
     * @return
     */
    String phone(String val);

    /**
     * 置空处理
     * @param val
     * @return
     */
    String blank(String val);
}
