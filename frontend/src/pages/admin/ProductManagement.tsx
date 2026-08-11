import {
  Box,
  Paper,
  Typography,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  IconButton,
  Chip,
  Button,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  MenuItem,
} from '@mui/material';
import { Edit, Delete, Add } from '@mui/icons-material';
import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useForm } from 'react-hook-form';
import { productsApi, categoriesApi } from '../../api/products';
import { CreateProductRequest } from '../../types';
import { LoadingSpinner } from '../../components/LoadingSpinner';
import { ErrorAlert } from '../../components/ErrorAlert';

export const ProductManagement = () => {
  const queryClient = useQueryClient();
  const [dialogOpen, setDialogOpen] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [nameFilter, setNameFilter] = useState('');

  // Trimmed before it enters the query key so "a " and "a" share one cache
  // entry and a whitespace-only term issues no filtered request. The server
  // trims again — it does not trust this.
  const appliedFilter = nameFilter.trim();

  const {
    register,
    handleSubmit,
    reset,
    setValue,
    formState: { errors },
  } = useForm<CreateProductRequest>();

  const { data: products, isLoading, error } = useQuery({
    queryKey: ['products', appliedFilter],
    queryFn: () => productsApi.getAll({ name: appliedFilter || undefined }),
    placeholderData: (previousData) => previousData,
  });

  const { data: categories } = useQuery({
    queryKey: ['categories'],
    queryFn: categoriesApi.getAll,
  });

  const createMutation = useMutation({
    mutationFn: productsApi.create,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['products'] });
      setDialogOpen(false);
      reset();
    },
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: number; data: CreateProductRequest }) =>
      productsApi.update(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['products'] });
      setDialogOpen(false);
      setEditingId(null);
      reset();
    },
  });

  const deleteMutation = useMutation({
    mutationFn: productsApi.delete,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['products'] });
    },
  });

  const handleEdit = (product: any) => {
    setEditingId(product.id);
    setValue('name', product.name);
    setValue('categoryId', product.categoryId);
    setValue('insurer', product.insurer || '');
    setValue('planType', product.planType || '');
    setValue('tags', product.tags || []);
    setDialogOpen(true);
  };

  const handleDelete = (id: number) => {
    if (window.confirm('Are you sure you want to delete this product?')) {
      deleteMutation.mutate(id);
    }
  };

  const onSubmit = (data: CreateProductRequest) => {
    // Parse tags from comma-separated string if needed
    const submitData = {
      ...data,
      tags: typeof data.tags === 'string'
        ? (data.tags as unknown as string).split(',').map((t: string) => t.trim()).filter(Boolean)
        : data.tags,
    };
    if (editingId) {
      updateMutation.mutate({ id: editingId, data: submitData });
    } else {
      createMutation.mutate(submitData);
    }
  };

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 3 }}>
        <Typography variant="h4">Manage Products</Typography>
        <Button
          variant="contained"
          startIcon={<Add />}
          onClick={() => {
            setEditingId(null);
            reset();
            setDialogOpen(true);
          }}
        >
          Add Product
        </Button>
      </Box>

      {/* Rendered unconditionally: if the loading branch replaced the whole
          page, this field would unmount on the first keystroke and lose focus. */}
      <Paper sx={{ mb: 2, p: 2 }}>
        <TextField
          label="Filter by name"
          variant="outlined"
          size="small"
          value={nameFilter}
          onChange={(e) => setNameFilter(e.target.value)}
          sx={{ minWidth: 300 }}
        />
      </Paper>

      {error ? (
        <ErrorAlert error={error as Error} />
      ) : isLoading ? (
        <LoadingSpinner message="Loading products..." />
      ) : (
        <TableContainer component={Paper}>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>Name</TableCell>
                <TableCell>Category</TableCell>
                <TableCell>Insurer</TableCell>
                <TableCell>Plan Type</TableCell>
                <TableCell>Tags</TableCell>
                <TableCell>Actions</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {products?.map((product) => (
                <TableRow key={product.id} hover>
                  <TableCell>{product.name}</TableCell>
                  <TableCell>{product.categoryName}</TableCell>
                  <TableCell>{product.insurer || '-'}</TableCell>
                  <TableCell>{product.planType || '-'}</TableCell>
                  <TableCell>
                    {product.tags?.map((tag, idx) => (
                      <Chip key={idx} label={tag} size="small" sx={{ mr: 0.5, mb: 0.5 }} />
                    )) || '-'}
                  </TableCell>
                  <TableCell>
                    <IconButton size="small" onClick={() => handleEdit(product)}>
                      <Edit />
                    </IconButton>
                    <IconButton
                      size="small"
                      color="error"
                      onClick={() => handleDelete(product.id)}
                    >
                      <Delete />
                    </IconButton>
                  </TableCell>
                </TableRow>
              ))}
              {products && products.length === 0 && (
                <TableRow>
                  <TableCell colSpan={6} align="center">
                    {appliedFilter
                      ? 'No products match the entered name.'
                      : 'No products found.'}
                  </TableCell>
                </TableRow>
              )}
            </TableBody>
          </Table>
        </TableContainer>
      )}

      {/* Create/Edit Dialog */}
      <Dialog
        open={dialogOpen}
        onClose={() => setDialogOpen(false)}
        maxWidth="md"
        fullWidth
      >
        <form onSubmit={handleSubmit(onSubmit)}>
          <DialogTitle>
            {editingId ? 'Edit Product' : 'Create Product'}
          </DialogTitle>
          <DialogContent>
            <TextField
              fullWidth
              label="Name"
              margin="normal"
              {...register('name', { required: 'Name is required' })}
              error={!!errors.name}
              helperText={errors.name?.message}
            />
            <TextField
              fullWidth
              select
              label="Category"
              margin="normal"
              defaultValue=""
              {...register('categoryId', { required: 'Category is required' })}
              error={!!errors.categoryId}
              helperText={errors.categoryId?.message}
            >
              {categories?.map((cat) => (
                <MenuItem key={cat.id} value={cat.id}>
                  {cat.name}
                </MenuItem>
              ))}
            </TextField>
            <TextField
              fullWidth
              label="Insurer"
              margin="normal"
              {...register('insurer', {
                required: 'Insurer is required',
              })}
              error={!!errors.insurer}
              helperText={errors.insurer?.message}
            />
            <TextField
              fullWidth
              label="Plan Type"
              margin="normal"
              {...register('planType', {
                required: 'Plan type is required',
              })}
              error={!!errors.planType}
              helperText={errors.planType?.message}
            />
            <TextField
              fullWidth
              label="Tags (comma-separated)"
              margin="normal"
              helperText="Enter tags separated by commas"
              {...register('tags')}
            />
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setDialogOpen(false)}>Cancel</Button>
            <Button type="submit" variant="contained">
              {editingId ? 'Update' : 'Create'}
            </Button>
          </DialogActions>
        </form>
      </Dialog>
    </Box>
  );
};
