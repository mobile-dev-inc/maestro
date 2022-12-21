import React, { useEffect, useState } from 'react';
import { DeviceScreen } from './models';
import { API, wait } from './api';

const InteractableDevice = () => {
  const [deviceScreen, setDeviceScreen] = useState<DeviceScreen>()
  const [error, setError] = useState<any>()

  useEffect(() => {
    (async () => {
      while (true) {
        try {
          const deviceScreen = await API.getDeviceScreen(true)
          setDeviceScreen(deviceScreen)
        } catch (e) {
          setError(e)
          await wait(1000)
        }
      }
    })()
  }, [setDeviceScreen, setError])

  if (!deviceScreen) return null
  return (
    <div
      className="h-full"
      style={{
        aspectRatio: deviceScreen.width / deviceScreen.height,
      }}
    >
      <img
        className="h-full pointer-events-none"
        src={deviceScreen.screenshot}
        alt="screenshot"
      />
    </div>
  )
}

export default InteractableDevice