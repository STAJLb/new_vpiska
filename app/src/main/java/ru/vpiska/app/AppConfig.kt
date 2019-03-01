package ru.vpiska.app

/**
 * Created by Кирилл on 09.10.2017.
 */

object AppConfig {

    private val URL_SERVER = "https://clickcoffee.ru/api/v2/"

    val URL_LOGIN = URL_SERVER + "users/login"

    val URL_REGISTER = URL_SERVER + "users/register"

    val URL_GET_PROFILE = URL_SERVER + "users/profile/"

    val URL_UPDATE_USER = URL_SERVER + "users/update"

    val URL_UPDATE_BALANCE = URL_SERVER + "users/update/balance"

    val URL_UPDATE_NUMBER_VIEW = URL_SERVER + "users/update/number-view"

    val URL_UPDATE_NUMBER_ADDING = URL_SERVER + "users/update/number-adding"

    val URL_ADD_FEEDBACK = URL_SERVER + "users/feedback"

    val URL_GET_PARTIES = URL_SERVER + "parties/"

    val URL_GET_PARTY = URL_SERVER + "parties/"

    val URL_ADD_PARTY = URL_SERVER + "parties"

    val URL_GET_DATA_OF_RATING = URL_SERVER + "ratings/"

    val URL_UPDATE_RATING_USER = URL_SERVER + "ratings/update/"

    val URL_UPDATE_NOTE = URL_SERVER + "notes/update/"

    val URL_GET_REVIEWS_OF_PARTY = URL_SERVER + "reviews/"

    val URL_ADD_REVIEW_TO_PARTY = URL_SERVER + "reviews"

    val URL_ADD_USER_TO_PARTY = URL_SERVER + "members"

    val URL_CHECK_SET_ANSWER = URL_SERVER + "members/check-set-answer"


    val URL_UPDATE_DATA_TOKENS = URL_SERVER + "tokens/update"

    val URL_CHECK_UPDATE_RATING = URL_SERVER + "ratings/check-update-rating"

    val URL_UPDATE_AVATAR = URL_SERVER + "avatars/update/image"

    val URL_CREATE_PURCHASE = URL_SERVER + "purchases"


    val URL_ADD_REPORT = URL_SERVER + "reports"
}