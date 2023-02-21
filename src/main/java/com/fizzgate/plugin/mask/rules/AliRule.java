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

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 支付宝隐私安全掩码处理工具类
 * see: https://opendocs.alipay.com/open/200/security
 */
@Component
public class AliRule implements MaskingRule {

    private static final int LEN_18 = 18;
    private static final int LEN_15 = 15;
    private static final int LEN_11 = 11;


    /**
     * 11位手机号显示前3位后2位
     * @param phone
     * @return
     */
    @Override
    public String phone(String phone) {
        if(StringUtils.isEmpty(phone)){
            return EMPTY_STR;
        }
        if(phone.length() == LEN_11){
            return hideChar(phone, 3, 2);
        }
        return defaultOut(phone);
    }

    /**
     * 返回空字符串
     * @param val
     * @return
     */
    @Override
    public String blank(String val) {
        return EMPTY_STR;
    }

    /**
     * 身份证掩码处理
     * @param idCard
     * @return
     */
    @Override
    public String idCard(String idCard) {
        if(StringUtils.isEmpty(idCard)){
            return EMPTY_STR;
        }

        if(idCard.length() == LEN_18 || idCard.length() == LEN_15){
            return hideChar(idCard, 1, 1);
        }else {
            return defaultOut(idCard);
        }
    }

    /**
     * 用户名掩码，规则：最后一个字符是中文，按中文处理只显示最后一个字符。
     * 否则按英文处理，英文姓名按照段数，2段或者以上的，脱敏第一段，否则只显示第一个字符
     * @param userName
     * @return
     */
    @Override
    public String name(String userName) {
        if(StringUtils.isEmpty(userName)){
            return EMPTY_STR;
        }
        // 去除首尾空格
        userName = userName.trim();
        // 中文处理
        int length = userName.length();
        char[] chars = userName.toCharArray();
        char lastChar = chars[length - 1];
        if(isChinese(lastChar)){
            if(length == 1){
                return String.valueOf(CHAR_STAR);
            }
            StringBuilder sb = new StringBuilder();
            for(int i = 0; i < length -1; i ++){
                if(chars[i] != ' '){
                    chars[i] = CHAR_STAR;
                }
                sb.append(chars[i]);
            }
            return sb.append(lastChar).toString();
        }
        // 英文处理
        String[] nameArr = userName.split(SPACE_STR);
        int enNameLength = nameArr.length;
        StringBuilder sb = new StringBuilder();
        if(enNameLength > 1){
            // 脱敏第一段
            sb.append(String.valueOf(CHAR_STAR));
            for(int i = 1; i < enNameLength; i++){
                sb.append(SPACE_STR).append(nameArr[i]);
            }
            return sb.toString();
        }
        // 只有一段
        if(userName.length() == 1){
            return userName;
        }

        return sb.append(userName.substring(0, 1)).append(CHAR_STAR).toString();
    }

    /**
     * 默认掩码规则：
     * 1，显示前 1/3 和后 1/3，其他用 * 号代替
     * 2，内容长度不能被 3 整除时，显示前 length/3.0 取整 + 1 和后 length/3.0 取整
     * @param str
     * @return
     */
    @Override
    public String defaultOut(String str){
        if(StringUtils.isEmpty(str)){
            return EMPTY_STR;
        }
        int length = str.length();
        boolean b = length % 3 == 0;
        int first, last;
        if(b){
            first = length/3;
            last = length/3;
        }else {
            first = length/3 + 1;
            last = length/3;
        }
        return hideChar(str, first, last);
    }

    /**
     * 掩码字符（使用*代替）
     * @param str 原字符串
     * @param first 需要显示的前n个
     * @param last 需要显示的最后n个
     * @return 掩码后的字符串
     */
    private static String hideChar(String str, int first, int last) {
        char[] chars = str.toCharArray();
        int length = chars.length;
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < length; i++){
            if(i >= first && i < length - last){
                chars[i] = CHAR_STAR;
            }
            sb.append(chars[i]);
        }
        return sb.toString();
    }

    /**
     * 根据字节码判断中文字符
     * @param c
     * @return
     */
    public static boolean isChinese(char c) {
        return c >= 0x4E00 &&  c <= 0x9FA5;
    }

}

