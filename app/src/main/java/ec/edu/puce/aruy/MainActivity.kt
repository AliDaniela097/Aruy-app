package ec.edu.puce.aruy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import ec.edu.puce.aruy.ui.theme.AruyTheme
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import ec.edu.puce.aruy.data.local.DatabaseProvider
import ec.edu.puce.aruy.data.repository.FavoritoRepository
import ec.edu.puce.aruy.ui.screen.FavoritosScreen
import ec.edu.puce.aruy.ui.viewmodel.FavoritoViewModel
import androidx.compose.foundation.layout.Box
import ec.edu.puce.aruy.data.remote.RetrofitClient
import ec.edu.puce.aruy.data.repository.UsuarioRepository
import ec.edu.puce.aruy.ui.viewmodel.UsuarioViewModel
import ec.edu.puce.aruy.ui.viewmodel.UsuarioViewModelFactory


import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import ec.edu.puce.aruy.ui.screen.UsuarioScreen

import ec.edu.puce.aruy.ui.navigation.PantallaAruy

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            AruyTheme {

                val database = remember {
                    DatabaseProvider.getDatabase(applicationContext)
                }

                val repository = remember(database) {
                    FavoritoRepository(database.favoritoDao())
                }

                val favoritoViewModel: FavoritoViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(
                            modelClass: Class<T>
                        ): T {
                            return FavoritoViewModel(repository) as T
                        }
                    }
                )

                val usuarioRepository = remember {
                    UsuarioRepository(RetrofitClient.usuarioApi)
                }

                val usuarioViewModel: UsuarioViewModel = viewModel(
                    factory = remember(usuarioRepository) {
                        UsuarioViewModelFactory(usuarioRepository)
                    }
                )

                var pantallaActual by rememberSaveable {
                    mutableStateOf(PantallaAruy.Favoritos.ruta)
                }

                Scaffold(
                    bottomBar = {
                        NavigationBar {

                            NavigationBarItem(
                                selected = pantallaActual == PantallaAruy.Favoritos.ruta,
                                onClick = {
                                    pantallaActual = PantallaAruy.Favoritos.ruta
                                },
                                icon = {
                                    Text("★")
                                },
                                label = {
                                    Text(PantallaAruy.Favoritos.titulo)
                                }
                            )

                            NavigationBarItem(
                                selected = pantallaActual == PantallaAruy.Usuario.ruta,
                                onClick = {
                                    pantallaActual = PantallaAruy.Usuario.ruta
                                },
                                icon = {
                                    Text("●")
                                },
                                label = {
                                    Text(PantallaAruy.Usuario.titulo)
                                }
                            )
                        }
                    }
                ) { innerPadding ->

                    Box(
                        modifier = Modifier.padding(innerPadding)
                    ) {

                        when (pantallaActual) {

                            PantallaAruy.Favoritos.ruta -> {
                                FavoritosScreen(favoritoViewModel)
                            }

                            PantallaAruy.Usuario.ruta -> {
                                UsuarioScreen(usuarioViewModel)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    AruyTheme {
        Greeting("Android")
    }
}