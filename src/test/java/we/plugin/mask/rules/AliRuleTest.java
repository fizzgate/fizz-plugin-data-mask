package we.plugin.mask.rules;

import org.junit.Test;
/**
 * @author pstoneone
 * @date 2021/9/23 19:11
 */
public class AliRuleTest {

    String phoneNum_11 = "15011327983";
    String phoneNumDefault = "88157923";

    String name_zh = "锦江酒店";
    String name_en = "JinJiang Online";

    String idcard_18 = "211330199010105551";
    String idcard_15 = "432815157233119";
    String idcardDefault = "2231898";

    String blankMock = "this is blank text";

    @Test
    public void aliMaskingTest () {
        AliRule aliRule = new AliRule();

        System.out.println("phoneNum_11 masking result is: " + aliRule.phone(phoneNum_11));
        System.out.println("phoneNumDefault masking result is: " + aliRule.phone(phoneNumDefault));
        System.out.println("name_zh masking result is: " + aliRule.name(name_zh));
        System.out.println("name_en masking result is: " + aliRule.name(name_en));
        System.out.println("idcard_18 masking result is: " + aliRule.idCard(idcard_18));
        System.out.println("idcard_15 masking result is: " + aliRule.idCard(idcard_15));
        System.out.println("idcardDefault masking result is: " + aliRule.idCard(idcardDefault));
        System.out.println("blankMock masking result is: " + aliRule.blank(blankMock));

    }
}
