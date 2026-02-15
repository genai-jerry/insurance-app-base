import {
  Box,
  Paper,
  Typography,
  Grid,
  Card,
  CardContent,
  CardActions,
  Button,
  Chip,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
} from '@mui/material';
import { ShoppingCart } from '@mui/icons-material';
import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { productsApi, categoriesApi } from '../../api/products';
import { LoadingSpinner } from '../../components/LoadingSpinner';
import { ErrorAlert } from '../../components/ErrorAlert';

export const ProductBrowser = () => {
  const [selectedCategory, setSelectedCategory] = useState<number | ''>('');

  const { data: categories, isLoading: categoriesLoading } = useQuery({
    queryKey: ['categories'],
    queryFn: categoriesApi.getAll,
  });

  const { data: products, isLoading: productsLoading, error } = useQuery({
    queryKey: ['products', selectedCategory],
    queryFn: () =>
      productsApi.getAll({
        categoryId: selectedCategory || undefined,
      }),
  });

  if (categoriesLoading || productsLoading) {
    return <LoadingSpinner message="Loading products..." />;
  }

  if (error) {
    return <ErrorAlert error={error as Error} />;
  }

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 3 }}>
        <Typography variant="h4">Product Browser</Typography>
        <FormControl sx={{ minWidth: 200 }}>
          <InputLabel>Category</InputLabel>
          <Select
            value={selectedCategory}
            label="Category"
            onChange={(e) => setSelectedCategory(e.target.value as number)}
          >
            <MenuItem value="">All Categories</MenuItem>
            {categories?.map((cat) => (
              <MenuItem key={cat.id} value={cat.id}>
                {cat.name}
              </MenuItem>
            ))}
          </Select>
        </FormControl>
      </Box>

      <Grid container spacing={3}>
        {products?.map((product) => (
          <Grid item xs={12} sm={6} md={4} key={product.id}>
            <Card sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
              <CardContent sx={{ flexGrow: 1 }}>
                <Typography variant="h6" gutterBottom>
                  {product.name}
                </Typography>
                <Chip
                  label={product.categoryName}
                  size="small"
                  sx={{ mb: 2 }}
                />
                {product.insurer && (
                  <Typography variant="body2" color="text.secondary" paragraph>
                    Insurer: {product.insurer}
                  </Typography>
                )}
                {product.planType && (
                  <Typography variant="body2" color="text.secondary">
                    Plan Type: {product.planType}
                  </Typography>
                )}
                {product.tags && product.tags.length > 0 && (
                  <Box sx={{ mt: 2 }}>
                    <Typography variant="subtitle2" gutterBottom>
                      Tags:
                    </Typography>
                    <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5 }}>
                      {product.tags.map((tag, idx) => (
                        <Chip
                          key={idx}
                          label={tag}
                          size="small"
                          variant="outlined"
                        />
                      ))}
                    </Box>
                  </Box>
                )}
              </CardContent>
              <CardActions>
                <Button size="small" startIcon={<ShoppingCart />} fullWidth>
                  Recommend to Lead
                </Button>
              </CardActions>
            </Card>
          </Grid>
        ))}
      </Grid>

      {products?.length === 0 && (
        <Paper sx={{ p: 4, textAlign: 'center' }}>
          <Typography color="text.secondary">
            No products found in this category
          </Typography>
        </Paper>
      )}
    </Box>
  );
};
