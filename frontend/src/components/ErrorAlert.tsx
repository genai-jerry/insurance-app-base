import { Alert, AlertTitle, Box } from '@mui/material';
import { AxiosError } from 'axios';

interface ErrorAlertProps {
  error: Error | AxiosError | string;
  onClose?: () => void;
}

export const ErrorAlert = ({ error, onClose }: ErrorAlertProps) => {
  const getErrorMessage = () => {
    if (typeof error === 'string') {
      return error;
    }

    if ('isAxiosError' in error && error.isAxiosError) {
      const axiosError = error as AxiosError<{ message?: string }>;
      return (
        axiosError.response?.data?.message ||
        axiosError.message ||
        'An unexpected error occurred'
      );
    }

    return error.message || 'An unexpected error occurred';
  };

  return (
    <Box sx={{ mb: 2 }}>
      <Alert severity="error" onClose={onClose}>
        <AlertTitle>Error</AlertTitle>
        {getErrorMessage()}
      </Alert>
    </Box>
  );
};
