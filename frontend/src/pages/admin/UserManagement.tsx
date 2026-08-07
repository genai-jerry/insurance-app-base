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
  TablePagination,
  IconButton,
  Chip,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  TextField,
  MenuItem,
} from '@mui/material';
import { Edit, Delete, PersonAdd } from '@mui/icons-material';
import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useForm } from 'react-hook-form';
import { adminApi } from '../../api/admin';
import { User, CreateUserRequest } from '../../types';
import { LoadingSpinner } from '../../components/LoadingSpinner';
import { ErrorAlert } from '../../components/ErrorAlert';
import { formatDate } from '../../utils/formatters';

export const UserManagement = () => {
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);
  const [rowsPerPage, setRowsPerPage] = useState(10);
  const [editDialogOpen, setEditDialogOpen] = useState(false);
  const [createDialogOpen, setCreateDialogOpen] = useState(false);
  const [selectedUser, setSelectedUser] = useState<User | null>(null);

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<Partial<User>>();

  const {
    register: registerCreate,
    handleSubmit: handleSubmitCreate,
    reset: resetCreate,
    formState: { errors: createErrors },
  } = useForm<CreateUserRequest>();

  const { data, isLoading, error } = useQuery({
    queryKey: ['users', page, rowsPerPage],
    queryFn: () => adminApi.getAllUsers({ page, size: rowsPerPage }),
  });

  const createMutation = useMutation({
    mutationFn: (data: CreateUserRequest) => adminApi.createUser(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['users'] });
      setCreateDialogOpen(false);
      resetCreate();
    },
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: number; data: Partial<User> }) =>
      adminApi.updateUser(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['users'] });
      setEditDialogOpen(false);
      reset();
    },
  });

  const deleteMutation = useMutation({
    mutationFn: adminApi.deleteUser,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['users'] });
    },
  });

  const handleEdit = (user: User) => {
    setSelectedUser(user);
    reset(user);
    setEditDialogOpen(true);
  };

  const handleDelete = (userId: number) => {
    if (window.confirm('Are you sure you want to delete this user?')) {
      deleteMutation.mutate(userId);
    }
  };

  const onSubmit = (data: Partial<User>) => {
    if (selectedUser) {
      updateMutation.mutate({ id: selectedUser.id, data });
    }
  };

  const onCreateSubmit = (data: CreateUserRequest) => {
    createMutation.mutate(data);
  };

  const handleOpenCreate = () => {
    resetCreate({ name: '', email: '', password: '', role: 'AGENT' });
    setCreateDialogOpen(true);
  };

  if (isLoading) {
    return <LoadingSpinner message="Loading users..." />;
  }

  if (error) {
    return <ErrorAlert error={error as Error} />;
  }

  return (
    <Box>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
        <Typography variant="h4">
          User Management
        </Typography>
        <Button
          variant="contained"
          startIcon={<PersonAdd />}
          onClick={handleOpenCreate}
        >
          Add User
        </Button>
      </Box>

      <TableContainer component={Paper}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>ID</TableCell>
              <TableCell>Name</TableCell>
              <TableCell>Email</TableCell>
              <TableCell>Role</TableCell>
              <TableCell>Created</TableCell>
              <TableCell>Actions</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {data?.map((user) => (
              <TableRow key={user.id} hover>
                <TableCell>{user.id}</TableCell>
                <TableCell>{user.name}</TableCell>
                <TableCell>{user.email}</TableCell>
                <TableCell>
                  <Chip
                    label={user.role}
                    size="small"
                    color={user.role === 'ADMIN' ? 'secondary' : 'primary'}
                  />
                </TableCell>
                <TableCell>{user.createdAt ? formatDate(user.createdAt) : '-'}</TableCell>
                <TableCell>
                  <IconButton size="small" onClick={() => handleEdit(user)}>
                    <Edit />
                  </IconButton>
                  <IconButton
                    size="small"
                    color="error"
                    onClick={() => handleDelete(user.id)}
                  >
                    <Delete />
                  </IconButton>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
        <TablePagination
          rowsPerPageOptions={[5, 10, 25]}
          component="div"
          count={data?.length || 0}
          rowsPerPage={rowsPerPage}
          page={page}
          onPageChange={(_, newPage) => setPage(newPage)}
          onRowsPerPageChange={(e) => {
            setRowsPerPage(parseInt(e.target.value, 10));
            setPage(0);
          }}
        />
      </TableContainer>

      {/* Create Dialog */}
      <Dialog
        open={createDialogOpen}
        onClose={() => setCreateDialogOpen(false)}
        maxWidth="sm"
        fullWidth
      >
        <form onSubmit={handleSubmitCreate(onCreateSubmit)}>
          <DialogTitle>Add New User</DialogTitle>
          <DialogContent>
            {createMutation.error && (
              <ErrorAlert error={createMutation.error as Error} />
            )}
            <TextField
              fullWidth
              label="Name"
              margin="normal"
              {...registerCreate('name', { required: 'Name is required' })}
              error={!!createErrors.name}
              helperText={createErrors.name?.message}
            />
            <TextField
              fullWidth
              label="Email"
              type="email"
              margin="normal"
              {...registerCreate('email', {
                required: 'Email is required',
                pattern: {
                  value: /^[^\s@]+@[^\s@]+\.[^\s@]+$/,
                  message: 'Invalid email',
                },
              })}
              error={!!createErrors.email}
              helperText={createErrors.email?.message}
            />
            <TextField
              fullWidth
              label="Password"
              type="password"
              margin="normal"
              {...registerCreate('password', {
                required: 'Password is required',
                minLength: {
                  value: 6,
                  message: 'Password must be at least 6 characters',
                },
              })}
              error={!!createErrors.password}
              helperText={createErrors.password?.message}
            />
            <TextField
              fullWidth
              select
              label="Role"
              margin="normal"
              defaultValue="AGENT"
              {...registerCreate('role', { required: 'Role is required' })}
            >
              <MenuItem value="AGENT">Agent</MenuItem>
              <MenuItem value="ADMIN">Admin</MenuItem>
            </TextField>
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setCreateDialogOpen(false)}>Cancel</Button>
            <Button
              type="submit"
              variant="contained"
              disabled={createMutation.isPending}
            >
              Create
            </Button>
          </DialogActions>
        </form>
      </Dialog>

      {/* Edit Dialog */}
      <Dialog
        open={editDialogOpen}
        onClose={() => setEditDialogOpen(false)}
        maxWidth="sm"
        fullWidth
      >
        <form onSubmit={handleSubmit(onSubmit)}>
          <DialogTitle>Edit User</DialogTitle>
          <DialogContent>
            {updateMutation.error && (
              <ErrorAlert error={updateMutation.error as Error} />
            )}
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
              label="Email"
              type="email"
              margin="normal"
              {...register('email', {
                required: 'Email is required',
                pattern: {
                  value: /^[^\s@]+@[^\s@]+\.[^\s@]+$/,
                  message: 'Invalid email',
                },
              })}
              error={!!errors.email}
              helperText={errors.email?.message}
            />
            <TextField
              fullWidth
              select
              label="Role"
              margin="normal"
              defaultValue={selectedUser?.role}
              {...register('role')}
            >
              <MenuItem value="AGENT">Agent</MenuItem>
              <MenuItem value="ADMIN">Admin</MenuItem>
            </TextField>
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setEditDialogOpen(false)}>Cancel</Button>
            <Button
              type="submit"
              variant="contained"
              disabled={updateMutation.isPending}
            >
              Save
            </Button>
          </DialogActions>
        </form>
      </Dialog>
    </Box>
  );
};
