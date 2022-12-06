import React, { useEffect, useState } from 'react';
import './App.css';

type UIElementBounds = {
  x: number
  y: number
  width: number
  height: number
}

type UIElement = {
  id: string
  bounds: UIElementBounds | null
  resourceId: string | null
  text: string | null
}

type Hierarchy = {
  screenshot: string
  elements: UIElement[]
}

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
      <img className="h-full" src={hierarchy.screenshot} alt="screenshot"/>
      <p className="overflow-scroll">{JSON.stringify(hierarchy.elements)}</p>
    </div>
  );
}

export default App;
