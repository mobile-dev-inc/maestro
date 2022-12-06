import { DivProps, Hierarchy } from './models';
import React from 'react';

export const AnnotatedScreenshot = ({hierarchy, ...rest}: {
  hierarchy: Hierarchy
} & DivProps) => {
  return (
    <div {...rest}>
      <img className="h-full" src={hierarchy.screenshot} alt="screenshot"/>
    </div>
  )
}