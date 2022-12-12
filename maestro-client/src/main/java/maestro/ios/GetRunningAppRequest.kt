package maestro.ios

import com.google.gson.annotations.SerializedName

data class GetRunningAppRequest(@SerializedName("appIds") val appIds: Set<String>)