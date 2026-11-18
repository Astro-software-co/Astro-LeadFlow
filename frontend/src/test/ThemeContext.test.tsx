import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { ThemeProvider, useTheme } from "@/contexts/ThemeContext";

function ThemeConsumer() {
  const { theme, setTheme, accentColor, setAccentColor } = useTheme();

  return (
    <div>
      <p data-testid="theme">{theme}</p>
      <p data-testid="accent">{accentColor}</p>
      <button onClick={() => setTheme("light")}>set-light</button>
      <button onClick={() => setTheme("dark")}>set-dark</button>
      <button onClick={() => setAccentColor("blue")}>set-blue</button>
      <button onClick={() => setAccentColor("green")}>set-green</button>
    </div>
  );
}

describe("ThemeContext", () => {
  it("starts with dark theme and purple accent by default", () => {
    render(
      <ThemeProvider>
        <ThemeConsumer />
      </ThemeProvider>,
    );

    expect(screen.getByTestId("theme")).toHaveTextContent("dark");
    expect(screen.getByTestId("accent")).toHaveTextContent("purple");
    expect(document.documentElement.classList.contains("dark")).toBe(true);
    expect(document.documentElement.getAttribute("data-accent")).toBe("purple");
  });

  it("updates theme and accent values through context setters", () => {
    render(
      <ThemeProvider>
        <ThemeConsumer />
      </ThemeProvider>,
    );

    fireEvent.click(screen.getByText("set-light"));
    expect(screen.getByTestId("theme")).toHaveTextContent("light");
    expect(document.documentElement.classList.contains("dark")).toBe(false);

    fireEvent.click(screen.getByText("set-dark"));
    expect(screen.getByTestId("theme")).toHaveTextContent("dark");
    expect(document.documentElement.classList.contains("dark")).toBe(true);

    fireEvent.click(screen.getByText("set-blue"));
    expect(screen.getByTestId("accent")).toHaveTextContent("blue");
    expect(document.documentElement.getAttribute("data-accent")).toBe("blue");

    fireEvent.click(screen.getByText("set-green"));
    expect(screen.getByTestId("accent")).toHaveTextContent("green");
    expect(document.documentElement.getAttribute("data-accent")).toBe("green");
  });

  it("throws when useTheme is used outside provider", () => {
    const spy = vi.spyOn(console, "error").mockImplementation(() => {});
    expect(() => render(<ThemeConsumer />)).toThrow("useTheme must be used within a ThemeProvider");
    spy.mockRestore();
  });
});
