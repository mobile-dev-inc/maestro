package maestro.cli.studio

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import maestro.cli.device.Device
import maestro.cli.device.DeviceService
import maestro.cli.session.MaestroSessionManager

private data class SelectDeviceRequest(
    val instanceId: String?,
    val modelId: String?,
)

object DevicesService {
    private var selectedDevice: Device.Connected? = null

    fun Route.devicesRoutes() {
        devicesRoute()
        getSelectedDeviceRoute()
        selectDeviceRoute()
    }

    private fun Route.devicesRoute() {
        get("/api/devices") {
            val connected = DeviceService.listConnectedDevices()
            val availableForLaunch = DeviceService.listAvailableForLaunchDevices()
            val data = Devices(connected, availableForLaunch)

            val response = jacksonObjectMapper()
                .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                .writerWithDefaultPrettyPrinter()
                .writeValueAsString(data)
            call.respondText(response)
        }
    }

    private fun Route.getSelectedDeviceRoute() {
        get("/api/devices/selected") {
            val response = jacksonObjectMapper()
                .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                .writerWithDefaultPrettyPrinter()
                .writeValueAsString(selectedDevice)
            call.respondText(response)
        }
    }

    private fun Route.selectDeviceRoute() {
        post("api/devices/select") {
            val request = call.parseBody<SelectDeviceRequest>()

            fun buildResponse(): String {
                return jacksonObjectMapper()
                    .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsString(selectedDevice)
            }

            if (selectedDevice?.instanceId == request.instanceId) {
                call.respondText(buildResponse())
            } else {
                var connectedDevice = DeviceService.listConnectedDevices()
                    .firstOrNull() { it.instanceId == request.instanceId }

                if (connectedDevice == null) {
                    val deviceAvailableForLaunch = DeviceService.listAvailableForLaunchDevices()
                        .firstOrNull() { it.modelId == request.modelId }
                    if (deviceAvailableForLaunch != null) {
                        connectedDevice = DeviceService.startDevice(deviceAvailableForLaunch)
                    } else {
                        throw HttpException(HttpStatusCode.BadRequest, "Specified device not found")
                    }

                }

                selectedDevice = connectedDevice

                call.respondText(buildResponse())

                MaestroSessionManager.newSession(null, null, connectedDevice.instanceId, true) { session ->
                    MaestroStudio.setMaestroInstance(session.maestro)
                }
            }
        }
    }
}