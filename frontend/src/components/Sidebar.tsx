import {
  Drawer,
  List,
  ListItem,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Toolbar,
  Divider,
  Box,
} from '@mui/material';
import {
  Dashboard,
  People,
  Phone,
  CalendarToday,
  ShoppingCart,
  RecordVoiceOver,
  Description,
  Settings,
  AdminPanelSettings,
  Category,
  FolderOpen,
  Assessment,
  ViewKanban,
} from '@mui/icons-material';
import { useNavigate, useLocation } from 'react-router-dom';
import { useAuthStore } from '../store/authStore';

const drawerWidth = 240;

interface SidebarProps {
  mobileOpen: boolean;
  onDrawerToggle: () => void;
}

export const Sidebar = ({ mobileOpen, onDrawerToggle }: SidebarProps) => {
  const navigate = useNavigate();
  const location = useLocation();
  const { user } = useAuthStore();

  const agentMenuItems = [
    { text: 'Dashboard', icon: <Dashboard />, path: '/agent/dashboard' },
    { text: 'Leads', icon: <People />, path: '/agent/leads' },
    { text: 'Kanban Board', icon: <ViewKanban />, path: '/agent/kanban' },
    { text: 'Call Calendar', icon: <CalendarToday />, path: '/agent/calendar' },
    { text: 'Products', icon: <ShoppingCart />, path: '/agent/products' },
  ];

  const adminMenuItems = [
    { text: 'Dashboard', icon: <Dashboard />, path: '/admin/dashboard' },
    { text: 'Users', icon: <AdminPanelSettings />, path: '/admin/users' },
    { text: 'Products', icon: <ShoppingCart />, path: '/admin/products' },
    { text: 'Categories', icon: <Category />, path: '/admin/categories' },
    { text: 'Documents', icon: <FolderOpen />, path: '/admin/documents' },
    { text: 'Model Config', icon: <Settings />, path: '/admin/config' },
    { text: 'Audit Logs', icon: <Assessment />, path: '/admin/audit' },
  ];

  const menuItems = user?.role === 'ADMIN' ? adminMenuItems : agentMenuItems;

  const drawer = (
    <Box>
      <Toolbar />
      <Divider />
      <List>
        {menuItems.map((item) => (
          <ListItem key={item.text} disablePadding>
            <ListItemButton
              selected={location.pathname === item.path}
              onClick={() => {
                navigate(item.path);
                if (mobileOpen) onDrawerToggle();
              }}
            >
              <ListItemIcon>{item.icon}</ListItemIcon>
              <ListItemText primary={item.text} />
            </ListItemButton>
          </ListItem>
        ))}
      </List>
    </Box>
  );

  return (
    <Box
      component="nav"
      sx={{ width: { sm: drawerWidth }, flexShrink: { sm: 0 } }}
    >
      {/* Mobile drawer */}
      <Drawer
        variant="temporary"
        open={mobileOpen}
        onClose={onDrawerToggle}
        ModalProps={{
          keepMounted: true, // Better open performance on mobile.
        }}
        sx={{
          display: { xs: 'block', sm: 'none' },
          '& .MuiDrawer-paper': {
            boxSizing: 'border-box',
            width: drawerWidth,
          },
        }}
      >
        {drawer}
      </Drawer>

      {/* Desktop drawer */}
      <Drawer
        variant="permanent"
        sx={{
          display: { xs: 'none', sm: 'block' },
          '& .MuiDrawer-paper': {
            boxSizing: 'border-box',
            width: drawerWidth,
          },
        }}
        open
      >
        {drawer}
      </Drawer>
    </Box>
  );
};
