import React from 'react';
import Main from './Main';
import { REAL_API } from './api';

function App() {
  const api = REAL_API
  return (
    <Main getDeviceScreen={api.getDeviceScreen} />
  );
}

export default App;
