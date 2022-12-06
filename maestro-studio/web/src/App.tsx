import React, { useEffect, useState } from 'react';
import './App.css';
import { DeviceScreen } from './models';
import { AnnotatedScreenshot } from './AnnotatedScreenshot';

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
    <div className="App flex h-full">
      <AnnotatedScreenshot
        className="h-full"
        deviceScreen={deviceScreen}
      />
    </div>
  );
}

export default App;
