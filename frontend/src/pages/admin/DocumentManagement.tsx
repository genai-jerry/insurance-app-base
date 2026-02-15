import {
  Box,
  Paper,
  Typography,
  Alert,
} from '@mui/material';
import { Description } from '@mui/icons-material';

export const DocumentManagement = () => {
  return (
    <Box>
      <Typography variant="h4" gutterBottom>
        Document Management
      </Typography>

      <Alert severity="info" sx={{ mb: 3 }}>
        Documents are managed through individual product pages. Navigate to a
        product to upload or manage its associated documents.
      </Alert>

      <Paper sx={{ p: 4, textAlign: 'center' }}>
        <Description sx={{ fontSize: 64, color: 'text.secondary', mb: 2 }} />
        <Typography variant="h6" color="text.secondary" gutterBottom>
          Product Documents
        </Typography>
        <Typography color="text.secondary">
          To manage documents, go to Product Management and select a product to
          view and upload its documents via the product document API.
        </Typography>
      </Paper>
    </Box>
  );
};
