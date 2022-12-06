import React, { useEffect, useState } from 'react';
import './App.css';
import { Hierarchy } from './models';
import { AnnotatedScreenshot } from './AnnotatedScreenshot';

function App() {
  const [hierarchy, setHierarchy] = useState<Hierarchy>()
  useEffect(() => {
    (async () => {
      const response = await fetch('/api/hierarchy')
      const hierarchy: Hierarchy = await response.json()
      setHierarchy(hierarchy)
    })()
  }, [])
  if (!hierarchy) {
    return (
      <div>Loading...</div>
    )
  }
  return (
    <div className="App flex h-full">
      <AnnotatedScreenshot
        className="h-full"
        hierarchy={hierarchy}
      />
      <p className="overflow-scroll">{JSON.stringify(hierarchy.elements)}</p>
    </div>
  );
}

export default App;
