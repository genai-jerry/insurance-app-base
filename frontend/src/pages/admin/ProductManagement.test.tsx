import { act, render, screen, waitFor, within } from '@testing-library/react';
import userEvent, { UserEvent } from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ProductManagement } from './ProductManagement';
import { productsApi, categoriesApi } from '../../api/products';
import { Category, Product } from '../../types';

vi.mock('../../api/products', () => ({
  productsApi: {
    getAll: vi.fn(),
    create: vi.fn(),
    update: vi.fn(),
    delete: vi.fn(),
  },
  categoriesApi: {
    getAll: vi.fn(),
  },
}));

const getAll = vi.mocked(productsApi.getAll);
const getAllCategories = vi.mocked(categoriesApi.getAll);

const CATEGORY: Category = {
  id: 1,
  name: 'Life',
  description: 'Life insurance',
};

const product = (id: number, name: string): Product => ({
  id,
  name,
  categoryId: CATEGORY.id,
  categoryName: CATEGORY.name,
  insurer: 'Acme Assurance',
  planType: 'TERM',
  tags: ['popular'],
  createdAt: '2026-01-01T00:00:00',
  updatedAt: '2026-01-01T00:00:00',
});

const TERM_LIFE = product(1, 'Term Life Secure');
const TERM_LIFE_PLUS = product(2, 'Term Life Plus');
const HEALTH = product(3, 'Family Health Shield');

const ALL_PRODUCTS = [TERM_LIFE, TERM_LIFE_PLUS, HEALTH];

const renderPage = () => {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return render(
    <QueryClientProvider client={queryClient}>
      <ProductManagement />
    </QueryClientProvider>
  );
};

const filterInput = () => screen.getByLabelText('Filter by name');

/**
 * Every keystroke starts a request, and each one settles between the
 * keystrokes user-event awaits internally. Without this outer act() those
 * resolutions land outside an act scope and React floods stderr with
 * "not wrapped in act" warnings even though the assertions pass.
 */
const typeFilter = (user: UserEvent, text: string) =>
  act(async () => {
    await user.type(filterInput(), text);
  });

const clearFilter = (user: UserEvent) =>
  act(async () => {
    await user.clear(filterInput());
  });

/** The params of the most recent listing request. */
const lastCallParams = () => getAll.mock.calls[getAll.mock.calls.length - 1][0];

/** The product names rendered in the table body, in render order. */
const renderedNames = () =>
  screen
    .getAllByRole('row')
    .slice(1)
    .map((row) => within(row).getAllByRole('cell')[0].textContent);

describe('ProductManagement name filter', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    getAll.mockResolvedValue(ALL_PRODUCTS);
    getAllCategories.mockResolvedValue([CATEGORY]);
  });

  it('sends the term to the server rather than filtering the fetched rows', async () => {
    // The scenario "the filter covers the whole catalogue, not just the
    // displayed rows" is only satisfied if the term reaches the listing
    // endpoint. This is the assertion that fails the moment someone
    // reimplements the filter as a client-side Array.filter over the rows
    // already fetched.
    const user = userEvent.setup();
    getAll.mockResolvedValue([TERM_LIFE, TERM_LIFE_PLUS]);
    renderPage();

    await screen.findByText('Term Life Secure');
    await typeFilter(user, 'Term Life');

    await waitFor(() =>
      expect(getAll).toHaveBeenLastCalledWith(
        expect.objectContaining({ name: 'Term Life' })
      )
    );

    // ...and the rows on screen are exactly what the server returned, in the
    // order it returned them — the component filtered nothing itself.
    await waitFor(() =>
      expect(renderedNames()).toEqual(['Term Life Secure', 'Term Life Plus'])
    );
  });

  it('renders every product the server returns for a partial term', async () => {
    const user = userEvent.setup();
    getAll.mockImplementation(async (params) =>
      params?.name ? ALL_PRODUCTS.filter((p) => p.name.includes('Term')) : ALL_PRODUCTS
    );
    renderPage();

    await screen.findByText('Family Health Shield');
    await typeFilter(user, 'Term');

    await waitFor(() =>
      expect(screen.queryByText('Family Health Shield')).not.toBeInTheDocument()
    );
    expect(renderedNames()).toEqual(['Term Life Secure', 'Term Life Plus']);
  });

  it('ignores whitespace surrounding the term', async () => {
    const user = userEvent.setup();
    getAll.mockResolvedValue([TERM_LIFE_PLUS]);
    renderPage();

    await screen.findByText('Term Life Plus');
    await typeFilter(user, '  Term Life Plus  ');

    // The field keeps what was typed; the request carries the trimmed term.
    expect(filterInput()).toHaveValue('  Term Life Plus  ');
    await waitFor(() =>
      expect(getAll).toHaveBeenLastCalledWith(
        expect.objectContaining({ name: 'Term Life Plus' })
      )
    );
  });

  it('issues no filtered request for a whitespace-only term', async () => {
    const user = userEvent.setup();
    renderPage();

    await screen.findByText('Term Life Secure');
    await typeFilter(user, '   ');

    await waitFor(() => expect(filterInput()).toHaveValue('   '));
    expect(getAll.mock.calls.every(([params]) => params?.name === undefined)).toBe(
      true
    );
  });

  it('restores the full list when the filter is cleared', async () => {
    const user = userEvent.setup();
    getAll.mockImplementation(async (params) =>
      params?.name ? [TERM_LIFE_PLUS] : ALL_PRODUCTS
    );
    renderPage();

    await screen.findByText('Family Health Shield');
    await typeFilter(user, 'Plus');

    await waitFor(() =>
      expect(screen.queryByText('Family Health Shield')).not.toBeInTheDocument()
    );

    await clearFilter(user);

    // No `name` value at all — an empty string would be a filter on "".
    await waitFor(() => expect(lastCallParams()?.name).toBeUndefined());
    expect(await screen.findByText('Family Health Shield')).toBeInTheDocument();
    expect(screen.getByText('Term Life Secure')).toBeInTheDocument();
    expect(screen.getByText('Term Life Plus')).toBeInTheDocument();
  });

  it('renders an explicit empty state when a filtered search matches nothing', async () => {
    const user = userEvent.setup();
    getAll.mockImplementation(async (params) => (params?.name ? [] : ALL_PRODUCTS));
    renderPage();

    await screen.findByText('Term Life Secure');
    await typeFilter(user, 'Nothing At All');

    expect(
      await screen.findByText('No products match the entered name.')
    ).toBeInTheDocument();
    expect(screen.queryByText('No products found.')).not.toBeInTheDocument();
  });

  it('keeps the unfiltered empty state when the catalogue itself is empty', async () => {
    getAll.mockResolvedValue([]);
    renderPage();

    expect(await screen.findByText('No products found.')).toBeInTheDocument();
    expect(
      screen.queryByText('No products match the entered name.')
    ).not.toBeInTheDocument();
  });

  it('keeps the filter field mounted and focused while requests are in flight', async () => {
    // A loading branch that replaced the whole page would unmount the field
    // after the first keystroke and drop keyboard focus.
    const user = userEvent.setup();
    renderPage();

    await screen.findByText('Term Life Secure');

    await act(async () => {
      await user.click(filterInput());
      await user.keyboard('Ter');
    });

    expect(filterInput()).toHaveFocus();
    expect(filterInput()).toHaveValue('Ter');
  });
});
