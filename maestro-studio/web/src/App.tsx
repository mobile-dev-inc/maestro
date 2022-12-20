import React from 'react';
import Main from './Main';
import { REAL_API } from './api';
import ReplView from './ReplView';

function App() {
  if (window.location.pathname === '/interact') {
    return (
      <ReplView api={REAL_API} />
    )
  }
  return (
    <Main getDeviceScreen={REAL_API.getDeviceScreen} />
  );
}

export default App;
