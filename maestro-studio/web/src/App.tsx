import React from 'react';
import Main from './Main';

function App() {
  const getDeviceScreen = async () => {
    const response = await fetch('/api/device-screen')
    return await response.json()
  }
  return (
    <Main getDeviceScreen={getDeviceScreen} />
  );
}

export default App;
