import { InputHint, InputWrapper, TextArea } from "../design-system/input";
import { TextAreaProps } from "../../const/models";

const CommandInput = ({
  value,
  setValue,
  error,
  onSubmit,
  ...rest
}: {
  value: string;
  setValue: (value: string) => void;
  error?: boolean | string | null;
  onSubmit?: () => void;
} & TextAreaProps) => {
  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.metaKey && e.key === "Enter") {
      onSubmit && onSubmit();
      return;
    } else if (e.key === "Enter") {
      e.preventDefault(); // Prevents the default action (newline/submit)
      const start = e.currentTarget.selectionStart;
      setValue(
        value.slice(0, start) + "\n" + value.slice(e.currentTarget.selectionEnd)
      );
    } else if (e.key === "Tab") {
      e.preventDefault(); // Prevents the default action (focus switch)
      const start = e.currentTarget.selectionStart;
      setValue(
        value.slice(0, start) +
          "    " +
          value.slice(e.currentTarget.selectionEnd)
      );
    }
  };

  return (
    <InputWrapper error={error}>
      <TextArea
        value={value}
        rows={2}
        resize="automatic"
        showResizeIcon={false}
        onChange={(e) => {
          setValue(e.target.value);
        }}
        onKeyDown={handleKeyDown}
        {...rest}
      />
      <InputHint />
    </InputWrapper>
  );
};

export default CommandInput;
