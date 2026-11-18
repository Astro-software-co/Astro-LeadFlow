import { beforeEach, describe, expect, it, vi } from "vitest";
import api from "@/lib/api";
import { vehiclesService } from "@/services/vehicles.service";

vi.mock("@/lib/api", () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    patch: vi.fn(),
    delete: vi.fn(),
  },
}));

const sampleVehicle = {
  id: "veh-1",
  brand: "Toyota",
  model: "Corolla",
  year: 2023,
  salePrice: 145_000,
  ipvaPaid: true,
  category: "SEDAN" as const,
  status: "AVAILABLE" as const,
  photos: [],
  organizationId: "org-1",
  createdAt: "2026-04-13T00:00:00.000Z",
  updatedAt: "2026-04-13T00:00:00.000Z",
};

describe("vehiclesService", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("list returns normalized pagination when backend responds with spring page format", async () => {
    vi.mocked(api.get).mockResolvedValueOnce({
      data: {
        content: [sampleVehicle],
        totalElements: 12,
        totalPages: 2,
        number: 1,
        size: 10,
      },
    });

    const result = await vehiclesService.list({ page: 1, size: 10, search: "corolla" });

    expect(api.get).toHaveBeenCalledWith("/vehicles", {
      params: { page: 1, size: 10, search: "corolla" },
    });
    expect(result).toEqual({
      content: [sampleVehicle],
      totalElements: 12,
      totalPages: 2,
      page: 1,
      size: 10,
    });
  });

  it("list falls back to array pagination when backend responds with array", async () => {
    vi.mocked(api.get).mockResolvedValueOnce({
      data: [sampleVehicle, { ...sampleVehicle, id: "veh-2", model: "Yaris" }],
    });

    const result = await vehiclesService.list({ page: 0, size: 1 });

    expect(result.totalElements).toBe(2);
    expect(result.totalPages).toBe(2);
    expect(result.page).toBe(0);
    expect(result.size).toBe(1);
    expect(result.content).toHaveLength(1);
    expect(result.content[0]?.id).toBe("veh-1");
  });

  it("getById fetches a vehicle by id", async () => {
    vi.mocked(api.get).mockResolvedValueOnce({ data: sampleVehicle });

    const result = await vehiclesService.getById("veh-1");

    expect(api.get).toHaveBeenCalledWith("/vehicles/veh-1");
    expect(result.id).toBe("veh-1");
  });

  it("create posts vehicle payload and returns created vehicle", async () => {
    const payload = {
      brand: "Toyota",
      model: "Corolla",
      year: 2023,
      salePrice: 145_000,
      category: "SEDAN" as const,
      status: "AVAILABLE" as const,
    };
    vi.mocked(api.post).mockResolvedValueOnce({ data: sampleVehicle });

    const result = await vehiclesService.create(payload);

    expect(api.post).toHaveBeenCalledWith("/vehicles", payload);
    expect(result.id).toBe("veh-1");
  });

  it("update puts payload on vehicle endpoint and returns updated vehicle", async () => {
    vi.mocked(api.put).mockResolvedValueOnce({
      data: { ...sampleVehicle, salePrice: 150_000 },
    });

    const result = await vehiclesService.update("veh-1", { salePrice: 150_000 });

    expect(api.put).toHaveBeenCalledWith("/vehicles/veh-1", { salePrice: 150_000 });
    expect(result.salePrice).toBe(150_000);
  });

  it("delete calls delete endpoint", async () => {
    vi.mocked(api.delete).mockResolvedValueOnce({});

    await vehiclesService.delete("veh-1");

    expect(api.delete).toHaveBeenCalledWith("/vehicles/veh-1");
  });

  it("syncPhotos removes deleted photos, uploads new ones and reorders the final gallery", async () => {
    const inlinePhoto =
      "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAusB9sot6mAAAAAASUVORK5CYII=";

    vi.mocked(api.get).mockResolvedValueOnce({
      data: [
        { id: "photo-1", s3Url: "https://cdn.test/existing-1.png", position: 0 },
        { id: "photo-2", s3Url: "https://cdn.test/existing-2.png", position: 1 },
      ],
    });
    vi.mocked(api.delete).mockResolvedValueOnce({});
    vi.mocked(api.post).mockResolvedValueOnce({
      data: { id: "photo-3", s3Url: "https://cdn.test/new-photo.png", position: 1 },
    });
    vi.mocked(api.patch).mockResolvedValueOnce({
      data: [
        { id: "photo-1", s3Url: "https://cdn.test/existing-1.png", position: 0 },
        { id: "photo-3", s3Url: "https://cdn.test/new-photo.png", position: 1 },
      ],
    });

    await vehiclesService.syncPhotos("veh-1", ["https://cdn.test/existing-1.png", inlinePhoto]);

    expect(api.get).toHaveBeenCalledWith("/vehicles/veh-1/photos");
    expect(api.delete).toHaveBeenCalledWith("/vehicles/veh-1/photos/photo-2");
    expect(api.post).toHaveBeenCalledWith(
      "/vehicles/veh-1/photos",
      expect.any(FormData),
      expect.objectContaining({
        headers: expect.objectContaining({
          "Content-Type": "multipart/form-data",
        }),
      }),
    );
    expect(api.patch).toHaveBeenCalledWith("/vehicles/veh-1/photos/reorder", {
      photoIds: ["photo-1", "photo-3"],
    });
  });

  it("syncPhotos preserves existing photos when signed urls rotate", async () => {
    vi.mocked(api.get).mockResolvedValueOnce({
      data: [
        {
          id: "photo-1",
          s3Url: "http://localhost:4566/leadflow-files/vehicles/org/veh/existing-1.jpg?X-Amz-Signature=new-signature",
          position: 0,
        },
      ],
    });
    vi.mocked(api.patch).mockResolvedValueOnce({
      data: [
        {
          id: "photo-1",
          s3Url: "http://localhost:4566/leadflow-files/vehicles/org/veh/existing-1.jpg?X-Amz-Signature=new-signature",
          position: 0,
        },
      ],
    });

    await vehiclesService.syncPhotos("veh-1", [
      "http://localhost:4566/leadflow-files/vehicles/org/veh/existing-1.jpg?X-Amz-Signature=old-signature",
    ]);

    expect(api.delete).not.toHaveBeenCalled();
    expect(api.post).not.toHaveBeenCalled();
    expect(api.patch).toHaveBeenCalledWith("/vehicles/veh-1/photos/reorder", {
      photoIds: ["photo-1"],
    });
  });
});
