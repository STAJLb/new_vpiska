package ru.vpiska.helper;

/**
 * Created by Кирилл on 19.03.2018.
 */

public class InAppBillingResources {

    private static final String RSA_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAgEHUGXIDz0NrmndC38xUBXjT3CCOzACTN05npMQKJKZfhLsA23udnhvBr2bvgLgmAwH/aNjFtFptJuoakPGt/LCDK2Ok8olg5ayLShj25smQ28JairU/SQ4egjy7V1ZkeFAp75dw4ri6OGLWV14tExNLT5nbWxUWbgStd6BPJJl9TaT/M/lnt+nx/bJwgfMlO/eQQetRQZQVlJB40LU5yv7j2MfBPh8OlvUcM02ZlCSa9utolAXmqAvMIo2OMoLI99ODDb8CkCU3U9pUdFme90A6FL5y1N1yOvdhypDRjuMYiQnwHPQdmmdRWR7ziP6iCoubNc3vN9fG2dmYsdi7qQIDAQAB"; // Ваш `RSA` ключ из `Google Play Developer Console`
    private static final String MERCHANT_ID = "14345126371516083485";           // Ваш `MERCHANT_ID` из `Google Play Developer Console`
    private static final String SKU_DISABLE_ADS = "ads_disable";          // Ваш `product_id`, создается в `Google Play Developer Console`


    public static String getRsaKey() {
        return RSA_KEY;
    }

    public static String getMerchantId() {
        return MERCHANT_ID;
    }

    public static String getSKU_Disable_Ads() {
        return SKU_DISABLE_ADS;
    }
}