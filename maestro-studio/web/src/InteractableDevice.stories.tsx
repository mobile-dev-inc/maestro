import InteractableDevice from './InteractableDevice';
import ReplView from './ReplView';
import React from 'react';

export default {
  title: 'InteractableDevice'
}

export const Main = () => {
  return (
    <InteractableDevice />
  )
}

export const WithRepl = () => {
  return (
    <div className="flex h-full overflow-hidden">
      <InteractableDevice />
      <ReplView />
    </div>
  )
}