import React, { useEffect, useState } from 'react';
import './App.css';
import { DeviceScreen } from './models';
import Inspect from './Inspect';

function App() {
  const [deviceScreen, setDeviceScreen] = useState<DeviceScreen>()
  useEffect(() => {
    (async () => {
      const response = await fetch('/api/device-screen')
      const hierarchy: DeviceScreen = await response.json()
      setDeviceScreen(hierarchy)
    })()
  }, [])
  if (!deviceScreen) {
    return (
      <div>Loading...</div>
    )
  }
  return (
    <Inspect deviceScreen={deviceScreen} />
  );
}

export default App;
