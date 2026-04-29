package com.speleo.start.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.speleo.start.data.local.entity.PersonEntity
import com.speleo.start.util.AgeCalculator

@Composable
fun PersonBadge(
    person: PersonEntity,
    mentorName: String? = null,
    onOpenCard: () -> Unit,
    onMentorClick: ((Long) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val age = AgeCalculator.calculateAge(person.birthDate)
    val ageMark = AgeCalculator.getAgeColorMark(person.birthDate)
    val ageColor = Color(android.graphics.Color.parseColor(ageMark.colorHex))

    val initial = person.lastName.take(1).uppercase()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Аватар
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initial,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ФИО
            Text(
                text = "${person.lastName} ${person.firstName}",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )

            // Возраст с цветом
            if (age != null) {
                Text(
                    text = "$age лет",
                    fontSize = 14.sp,
                    color = ageColor
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Ментор (если есть)
            if (mentorName != null) {
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))
                TextButton(onClick = { /* mentor click */ }) {
                    Text("👨‍🏫 Ментор: $mentorName")
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))

            // Ссылка на карточку
            TextButton(onClick = onOpenCard) {
                Text("📄 Открыть карточку")
            }
        }
    }
}