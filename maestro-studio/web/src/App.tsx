import React from 'react';
import Main from './Main';
import { REAL_API } from './api';
import ReplView from './ReplView';

function App() {
  const api = REAL_API
  // TODO delete this
  if (true) {
    return (
      <ReplView api={api} />
    )
  }
  return (
    <Main getDeviceScreen={api.getDeviceScreen} />
  );
}

export default App;
