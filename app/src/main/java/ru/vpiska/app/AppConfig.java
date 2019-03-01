package ru.vpiska.app;

/**
 * Created by Кирилл on 09.10.2017.
 */

public class AppConfig {

    private final static String URL_SERVER = "https://clickcoffee.ru/api/v2/";

    public final static String URL_LOGIN = URL_SERVER + "users/login";

    public final static String URL_LOGIN_GUEST = URL_SERVER + "guests/login";

    public final static String URL_REGISTER = URL_SERVER + "users/register";

    public final static String URL_GET_PROFILE = URL_SERVER + "users/profile/";

    public final static String URL_UPDATE_USER = URL_SERVER + "users/update";

    public final static String URL_UPDATE_BALANCE = URL_SERVER + "users/update/balance";

    public final static String URL_UPDATE_NUMBER_VIEW = URL_SERVER + "users/update/number-view";

    public final static String URL_UPDATE_NUMBER_ADDING = URL_SERVER + "users/update/number-adding";

    public final static String URL_ADD_FEEDBACK = URL_SERVER + "users/feedback";

    public final static String URL_GET_PARTIES = URL_SERVER + "parties/";

    public final static String URL_GET_PARTY = URL_SERVER + "parties/";

    public final static String URL_ADD_PARTY = URL_SERVER + "parties";

    public final static String URL_GET_DATA_OF_RATING = URL_SERVER + "ratings/";

    public final static String URL_UPDATE_RATING_USER = URL_SERVER + "ratings/update/";

    public final static String URL_UPDATE_NOTE = URL_SERVER + "notes/update/";

    public final static String URL_GET_REVIEWS_OF_PARTY = URL_SERVER + "reviews/";

    public final static String URL_ADD_REVIEW_TO_PARTY = URL_SERVER + "reviews";

    public final static String URL_ADD_USER_TO_PARTY = URL_SERVER + "members";

    public final static String URL_CHECK_SET_ANSWER = URL_SERVER + "members/check-set-answer";



    public final static String URL_UPDATE_DATA_TOKENS = URL_SERVER + "tokens/update";

    public final static String URL_CHECK_UPDATE_RATING = URL_SERVER + "ratings/check-update-rating";

    public final static String URL_UPDATE_AVATAR = URL_SERVER + "avatars/update/image";

    public final static String URL_CREATE_PURCHASE = URL_SERVER + "purchases";




    public final static String URL_ADD_REPORT = URL_SERVER + "reports";
}