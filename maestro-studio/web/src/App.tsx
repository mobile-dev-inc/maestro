import React from 'react';
import Main from './Main';
import ReplView from './ReplView';
import InteractableDevice from './InteractableDevice';

function App() {
  if (window.location.pathname === '/interact') {
    return (
      <div className="flex h-full overflow-hidden">
        <InteractableDevice />
        <ReplView />
      </div>
    )
  }
  return (
    <Main />
  );
}

export default App;
