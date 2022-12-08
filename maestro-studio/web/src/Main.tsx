import Inspect from './Inspect';
import React, { useEffect, useState } from 'react';
import { DeviceScreen } from './models';

const Main = ({ getDeviceScreen }: {
  getDeviceScreen: () => Promise<DeviceScreen>
}) => {
  const [deviceScreen, setDeviceScreen] = useState<DeviceScreen>()

  const refresh = async () => {
    const deviceScreen = await getDeviceScreen()
    setDeviceScreen(deviceScreen)
  }

  useEffect(() => {
    refresh()
  }, [])

  if (!deviceScreen) {
    return (
      <div>Loading...</div>
    )
  }

  return (
    <Inspect deviceScreen={deviceScreen} />
  )
}

export default Main