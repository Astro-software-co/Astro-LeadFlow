import { fireEvent, render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { NotificationProvider, useNotifications } from "@/contexts/NotificationContext";

function NotificationConsumer() {
  const {
    notifications,
    unreadCount,
    addNotification,
    markAsRead,
    markAllAsRead,
    removeNotification,
    clearAll,
  } = useNotifications();

  return (
    <div>
      <p data-testid="total">{notifications.length}</p>
      <p data-testid="unread">{unreadCount}</p>
      <button
        onClick={() =>
          addNotification({
            title: "Novo Lead",
            message: "Lead do Webmotors",
            type: "info",
          })
        }
      >
        add
      </button>
      <button onClick={() => markAsRead("1")}>mark-1</button>
      <button onClick={markAllAsRead}>mark-all</button>
      <button onClick={() => removeNotification("2")}>remove-2</button>
      <button onClick={clearAll}>clear</button>
    </div>
  );
}

describe("NotificationContext", () => {
  beforeEach(() => {
    vi.spyOn(globalThis.crypto, "randomUUID").mockReturnValue("mock-notification-id");
  });

  it("starts with seeded notifications and unread count", () => {
    render(
      <NotificationProvider>
        <NotificationConsumer />
      </NotificationProvider>,
    );

    expect(screen.getByTestId("total")).toHaveTextContent("3");
    expect(screen.getByTestId("unread")).toHaveTextContent("2");
  });

  it("addNotification prepends item and increases unread count", () => {
    render(
      <NotificationProvider>
        <NotificationConsumer />
      </NotificationProvider>,
    );

    fireEvent.click(screen.getByText("add"));
    expect(screen.getByTestId("total")).toHaveTextContent("4");
    expect(screen.getByTestId("unread")).toHaveTextContent("3");
  });

  it("markAsRead and markAllAsRead update unread counter", () => {
    render(
      <NotificationProvider>
        <NotificationConsumer />
      </NotificationProvider>,
    );

    fireEvent.click(screen.getByText("mark-1"));
    expect(screen.getByTestId("unread")).toHaveTextContent("1");

    fireEvent.click(screen.getByText("mark-all"));
    expect(screen.getByTestId("unread")).toHaveTextContent("0");
  });

  it("removeNotification and clearAll change list size", () => {
    render(
      <NotificationProvider>
        <NotificationConsumer />
      </NotificationProvider>,
    );

    fireEvent.click(screen.getByText("remove-2"));
    expect(screen.getByTestId("total")).toHaveTextContent("2");

    fireEvent.click(screen.getByText("clear"));
    expect(screen.getByTestId("total")).toHaveTextContent("0");
    expect(screen.getByTestId("unread")).toHaveTextContent("0");
  });

  it("throws when useNotifications is used outside provider", () => {
    const spy = vi.spyOn(console, "error").mockImplementation(() => {});
    expect(() => render(<NotificationConsumer />)).toThrow(
      "useNotifications must be used within a NotificationProvider",
    );
    spy.mockRestore();
  });
});
