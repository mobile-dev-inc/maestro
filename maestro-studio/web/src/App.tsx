import React, { ReactNode, useState } from 'react';
import DeprecatedInspectPage from './DeprecatedInspectPage';
import InteractPage from './InteractPage';
import MockPage from './MockPage';

type Mode = 'interact' | 'mock'

const ModeButton = ({isActive, onClick, children}: {isActive: boolean, onClick: () => void, children: ReactNode}) => (
  <div 
  className={`cursor-pointer mr-2 p-2 rounded-md ${isActive ? 'bg-slate-200' : 'bg-slate-50'}`} 
  onClick={onClick}
  >{children}</div>
)

function App() {
  const [mode, setMode] = useState<Mode>('mock')

  if (window.location.pathname === '/inspect') {
    return (
      <DeprecatedInspectPage />
    )
  }

  const page = mode === 'mock' ? <MockPage /> : <InteractPage />

  return (
    <div className="flex flex-col h-full">
      <div className="flex flex-row justify-center">
        <ModeButton isActive={mode === 'interact'} onClick={() => setMode('interact')}>Interact page</ModeButton>
        <ModeButton isActive={mode === 'mock'} onClick={() => setMode('mock')}>Mock page</ModeButton>
      </div>
      {page}
    </div>
  );
}

export default App;
