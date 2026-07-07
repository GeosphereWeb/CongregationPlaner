package de.geosphere.congregationplaner.ui.button

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.padding

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
fun PrimaryButtonPreview() {
    MaterialTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            PrimaryButton(text = "Click me", onClick = {})
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
fun SecondaryButtonPreview() {
    MaterialTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            SecondaryButton(text = "Click me", onClick = {})
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
fun OutlinedButtonPreview() {
    MaterialTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            OutlinedButton(text = "Click me", onClick = {})
        }
    }
}
