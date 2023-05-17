package maestro.utils

import maestro.DeviceInfo
import maestro.MaestroException
import maestro.Point

class PointUtils {
    companion object {
        fun getPoint(pointString: String, deviceInfo: DeviceInfo): Point {
            return if (pointString.contains("%")) {
                val (percentX, percentY) = pointString
                    .replace("%", "")
                    .split(",")
                    .map { it.trim().toInt() }

                if (percentX !in 0..100 || percentY !in 0..100) {
                    throw MaestroException.InvalidCommand("Invalid point: $pointString")
                }

                val x = deviceInfo.widthGrid * percentX / 100
                val y = deviceInfo.heightGrid * percentY / 100

                Point(x, y)
            } else {
                val (x, y) = pointString.split(",")
                    .map {
                        it.trim().toInt()
                    }
                Point(x, y)
            }
        }
    }
}