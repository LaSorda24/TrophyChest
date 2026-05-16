package com.gonzalez.trophychest.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.gonzalez.trophychest.R
import com.gonzalez.trophychest.navigation.Screen
import com.gonzalez.trophychest.ui.theme.ReadableTertiary

data class NavigationItem(//CLASE PARA LA BARRA DE NAVEGACION
    val screen: Screen,
    val iconUnselected: Int,
    val iconSelected: Int,//ICONO SELECCIONADO
    val label: String//DESCRIPCION
)

@Composable
fun BottomNavigationBar(navController: NavHostController) {

    val items = listOf(//LISTA DE ICONOS
        NavigationItem(Screen.Principal, R.drawable.home_borde, R.drawable.home_lleno, "Inicio"),
        NavigationItem(Screen.Mensajes, R.drawable.amigo_borde, R.drawable.amigo_lleno, "Chat"),
        NavigationItem(Screen.Busqueda, R.drawable.buscar_borde, R.drawable.buscar_lleno, "Buscar"),
        NavigationItem(Screen.Trofeos, R.drawable.trofeo_borde, R.drawable.trofeo_lleno, "Trofeos"),
        NavigationItem(Screen.Perfil, R.drawable.perfil_borde, R.drawable.perfil_lleno, "Perfil"),
    )

    NavigationBar(//CREAMOS BARRA INFERIOR
        containerColor = Color(0xFF121212),
        contentColor = Color(0xFFFFD700)
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()//LE DAMOS EL ESTADO DE LA NAVEGACION
        val currentRoute = navBackStackEntry?.destination?.route//LE ASIGNAMOS LA RUTA ACTUAL A navBackStackEntry

        items.forEach { item ->
            // AQUI MARCO EL ICONO ACTIVO SEGUN LA PANTALLA EN LA QUE ESTOY
            val isSelected = currentRoute == item.screen.route
            val iconAsset = if (isSelected) item.iconSelected //AQUI REVISA QUE ESTE SELECCIONADO O NO
            else item.iconUnselected

            NavigationBarItem(//CREAMOS EL BOTON EN SI
                selected = isSelected,
                label = null,
                alwaysShowLabel = false,
                icon = {
                    Icon(
                        painter = painterResource(id = iconAsset),
                        contentDescription = item.label,
                        modifier = Modifier.size(32.dp)
                    )
                },
                onClick = {
                    // CON ESTO PUEDO VOLVER AL INICIO INCLUSO DESDE UNA PANTALLA DE DETALLE
                    if (currentRoute != item.screen.route) {//SE MUEVE SI PULSAS UNA OPCION DIFERENTE
                        navController.navigate(item.screen.route) {
                            // AQUI REUTILIZO EL HISTORIAL PARA QUE LA APP NO SE LLENE DE PANTALLAS REPETIDAS
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true//EVITA DUPLICADOS DE PANTALLA
                            restoreState = true//RESTAURA ESTADO ANTERIOR
                        }
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.White,
                    unselectedIconColor = ReadableTertiary,
                    indicatorColor = Color.Transparent
                )
            )
        }
    }
}
