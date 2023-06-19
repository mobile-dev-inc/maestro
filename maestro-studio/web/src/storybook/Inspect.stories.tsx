import React from "react";
import Inspect from "../components/depreciated(toremove)/Inspect";
import { mockDeviceScreen } from "../api/mocks";

export default {
  title: "Inspect",
};

export const Main = () => {
  return <Inspect deviceScreen={mockDeviceScreen} refresh={() => {}} />;
};
