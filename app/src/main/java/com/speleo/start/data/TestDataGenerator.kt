package com.speleo.start.data

import android.util.Log
import androidx.room.withTransaction
import com.speleo.start.data.local.AppDatabase
import com.speleo.start.data.local.dao.AppSettingsDao
import com.speleo.start.data.local.dao.CompetitionDao
import com.speleo.start.data.local.dao.MasterRouteCardDao
import com.speleo.start.data.local.dao.MentorDao
import com.speleo.start.data.local.dao.ParticipantDao
import com.speleo.start.data.local.dao.PersonDao
import com.speleo.start.data.local.dao.TeamDao
import com.speleo.start.data.local.entity.CompetitionEntity
import com.speleo.start.data.local.entity.MasterRouteCardEntity
import com.speleo.start.data.local.entity.MentorEntity
import com.speleo.start.data.local.entity.ParticipantEntity
import com.speleo.start.data.local.entity.PersonEntity
import com.speleo.start.data.local.entity.TeamEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Random
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TestDataGenerator @Inject constructor(
    private val db: AppDatabase,
    private val competitionDao: CompetitionDao,
    private val personDao: PersonDao,
    private val teamDao: TeamDao,
    private val participantDao: ParticipantDao,
    private val mentorDao: MentorDao,
    private val masterRouteCardDao: MasterRouteCardDao,
    private val appSettingsDao: AppSettingsDao
) {
    private val random = Random(42)

    suspend fun generate() {
        withContext(Dispatchers.IO) {
            clearAll()

            db.withTransaction {
                try {
                    // 1. СОРЕВНОВАНИЕ
                    val compId = competitionDao.insert(
                        CompetitionEntity(
                            name = "Бяковские каменоломни 2026",
                            shortName = "Бяки 2026",
                            date = "13.12.2025",
                            place = "Тульская обл., пос. Метростроевский",
                            discipline = "underground",
                            system = "Бяковская (Бяки)",
                            settingsJson = "{\"start_interval\":60,\"control_time_2\":90,\"control_time_3\":60,\"min_team_size\":2}"
                        )
                    )
                    if (compId == -1L) {
                        Log.e("TestDataGenerator", "❌ Не удалось создать соревнование")
                        return@withTransaction
                    }
                    Log.d("TestDataGenerator", "✅ Создано соревнование ID: $compId")

                    // 2. КОНТРОЛЬНЫЕ ПУНКТЫ (12 штук)
                    for (i in 1..12) {
                        val cp = MasterRouteCardEntity(
                            competitionId = compId,
                            displayNumber = i,
                            weight = 2 + random.nextInt(14),
                            type = if (i % 4 == 0) "technical" else "normal",
                            sortOrder = i,
                            normativeSeconds = if (i % 4 == 0) 60 + random.nextInt(241) else 0,
                            forClass2 = true,
                            forClass3 = i <= 10,
                            trackWaitTime = i % 4 == 0,
                            trackExecutionTime = false,
                            bonusPoints = 0
                        )
                        masterRouteCardDao.insert(cp)
                    }
                    Log.d("TestDataGenerator", "✅ Создано 12 КП")

                    // 3. ПЕРСОНЫ (40 штук)
                    val maleFirst = listOf("Александр","Дмитрий","Максим","Сергей","Андрей","Алексей","Иван","Пётр","Николай","Владимир","Артём","Даниил","Егор","Матвей","Тимофей","Роман","Кирилл","Михаил","Илья","Павел")
                    val femaleFirst = listOf("Анна","Мария","Елена","Ольга","Татьяна","Наталья","Екатерина","Ирина","Светлана","Юлия","Евгения","Анастасия","Дарья","Полина","Виктория","Алина","Валерия","София","Алиса","Вероника")
                    val maleLast = listOf("Иванов","Петров","Сидоров","Смирнов","Кузнецов","Попов","Васильев","Михайлов","Новиков","Фёдоров","Соколов","Лебедев","Козлов","Волков","Зайцев","Соловьёв","Борисов","Тимофеев","Григорьев","Егоров")
                    val femaleLast = listOf("Иванова","Петрова","Сидорова","Смирнова","Кузнецова","Попова","Васильева","Михайлова","Новикова","Фёдорова","Соколова","Лебедева","Козлова","Волкова","Зайцева","Соловьёва","Борисова","Тимофеева","Григорьева","Егорова")
                    val nicks = listOf("Геолог","Спелеолог","Фонарик","Компас","Штольня","Каска","Верёвка","Камень","ЛетучаяМышь","Подземка","Шахтёр","Рудокоп","Сталкер","Крот","Бункер","Каньон","Сапёр","Маяк","Азимут","Горизонт","Тоннель","Плутон","Страж","Феникс","Тень")

                    val personIds = mutableListOf<Long>()
                    for (i in 0..39) {
                        val isMale = i < 20
                        val fn = if (isMale) maleFirst[i % maleFirst.size] else femaleFirst[i % femaleFirst.size]
                        val ln = if (isMale) maleLast[i % maleLast.size] else femaleLast[i % femaleLast.size]

                        // Разброс возрастов: несколько детей (2008-2016), подростки (2005-2007), взрослые (1985-2004)
                        val year = when {
                            i in 0..4 -> 2016 + i  // дети: 2016-2020
                            i in 5..9 -> 2008 + (i - 5)  // подростки: 2008-2012
                            else -> 1985 + random.nextInt(20)  // взрослые: 1985-2004
                        }

                        val bm = 1 + random.nextInt(12)
                        val bd = 1 + random.nextInt(28)
                        val birth = "%02d.%02d.%04d".format(bd, bm, year)
                        val phone = "+7 9%02d %03d-%02d-%02d".format(10+random.nextInt(90), 100+random.nextInt(900), 10+random.nextInt(90), 10+random.nextInt(90))

                        val pid = personDao.insert(
                            PersonEntity(
                                lastName = ln,
                                firstName = fn,
                                middleName = if(random.nextBoolean()) "${fn}ович" else null,
                                nickname = nicks[i % nicks.size],
                                birthDate = birth,
                                phone = phone,
                                gender = if(isMale) "male" else "female"
                            )
                        )
                        if (pid > 0) {
                            personIds.add(pid)
                        }
                    }
                    Log.d("TestDataGenerator", "✅ Создано ${personIds.size} персон")

                    // 4. МЕНТОРЫ (15 человек, все совершеннолетние)
                    for (i in 0..14) {
                        if (i < personIds.size) {
                            mentorDao.insert(MentorEntity(personId = personIds[i]))
                        }
                    }
                    Log.d("TestDataGenerator", "✅ Создано 15 менторов")

                    // 5. КОМАНДЫ 2-го класса (15 команд, все в статусе registered)
                    for (t in 1..15) {
                        val tid = teamDao.insert(
                            TeamEntity(
                                competitionId = compId,
                                teamNumber = t,
                                className = "2",
                                status = "registered",
                                colorMark = "green"
                            )
                        )
                        if (tid == -1L) continue

                        // 2-3 участника в команде
                        val memberCount = 2 + random.nextInt(2)
                        val usedPersonIds = mutableSetOf<Long>()

                        for (m in 0 until memberCount) {
                            var pid = personIds[(t * 2 + m) % personIds.size]
                            // Защита от дублирования участников в одной команде
                            while (pid in usedPersonIds) {
                                pid = personIds[(pid.toInt() + 1) % personIds.size]
                            }
                            usedPersonIds.add(pid)

                            val role = if (m == 0) "captain" else "member"
                            val person = personDao.getPersonById(pid)
                            val age = calculateAge(person?.birthDate)

                            // Для несовершеннолетних случайно назначаем ментора
                            val mentorId = if (age != null && age < 18 && random.nextBoolean()) {
                                val mentorPerson = personDao.getPersonById(personIds[random.nextInt(15)])
                                mentorDao.getMentorByPersonId(mentorPerson?.id ?: -1L)?.id
                            } else null

                            participantDao.insert(
                                ParticipantEntity(
                                    teamId = tid,
                                    personId = pid,
                                    role = role,
                                    status = "active",
                                    mentorId = mentorId,
                                    mentorConfirmed = mentorId != null,
                                    judgeApproved = (age != null && age < 14) && random.nextBoolean()
                                )
                            )
                        }
                    }
                    Log.d("TestDataGenerator", "✅ Создано 15 команд 2-го класса")

                    // 6. КОМАНДЫ 3-го класса (15 команд, все в статусе registered)
                    for (t in 1..15) {
                        val tid = teamDao.insert(
                            TeamEntity(
                                competitionId = compId,
                                teamNumber = t,
                                className = "3",
                                status = "registered",
                                colorMark = "green"
                            )
                        )
                        if (tid == -1L) continue

                        val memberCount = 2 + random.nextInt(2)
                        val usedPersonIds = mutableSetOf<Long>()

                        for (m in 0 until memberCount) {
                            var pid = personIds[(t * 3 + 20 + m) % personIds.size]
                            while (pid in usedPersonIds) {
                                pid = personIds[(pid.toInt() + 1) % personIds.size]
                            }
                            usedPersonIds.add(pid)

                            val role = if (m == 0) "captain" else "member"
                            val person = personDao.getPersonById(pid)
                            val age = calculateAge(person?.birthDate)

                            val mentorId = if (age != null && age < 18 && random.nextBoolean()) {
                                val mentorPerson = personDao.getPersonById(personIds[random.nextInt(15)])
                                mentorDao.getMentorByPersonId(mentorPerson?.id ?: -1L)?.id
                            } else null

                            participantDao.insert(
                                ParticipantEntity(
                                    teamId = tid,
                                    personId = pid,
                                    role = role,
                                    status = "active",
                                    mentorId = mentorId,
                                    mentorConfirmed = mentorId != null,
                                    judgeApproved = (age != null && age < 14) && random.nextBoolean()
                                )
                            )
                        }
                    }
                    Log.d("TestDataGenerator", "✅ Создано 15 команд 3-го класса")

                    Log.d("TestDataGenerator", "🎉 Тестовые данные успешно сгенерированы! Всего команд: 30")
                } catch (e: Exception) {
                    Log.e("TestDataGenerator", "❌ Ошибка при генерации: ${e.message}", e)
                }
            }
        }
    }

    private fun calculateAge(birthDate: String?): Int? {
        if (birthDate.isNullOrBlank() || birthDate.length != 10) return null
        return try {
            val day = birthDate.substring(0, 2).toInt()
            val month = birthDate.substring(3, 5).toInt()
            val year = birthDate.substring(6, 10).toInt()

            val now = java.util.Calendar.getInstance()
            val birth = java.util.Calendar.getInstance().apply { set(year, month - 1, day) }

            var age = now.get(java.util.Calendar.YEAR) - birth.get(java.util.Calendar.YEAR)
            if (now.get(java.util.Calendar.DAY_OF_YEAR) < birth.get(java.util.Calendar.DAY_OF_YEAR)) {
                age--
            }
            age
        } catch (e: Exception) {
            null
        }
    }

    suspend fun clearAll() {
        withContext(Dispatchers.IO) {
            try {
                appSettingsDao.delete("competition_start_timestamp")
                appSettingsDao.delete("competition_active")

                participantDao.deleteAll()
                mentorDao.deleteAll()
                masterRouteCardDao.deleteAll()
                teamDao.deleteAll()
                personDao.deleteAll()
                competitionDao.deleteAll()

                Log.d("TestDataGenerator", "✅ Все данные очищены")
            } catch (e: Exception) {
                Log.e("TestDataGenerator", "❌ Ошибка при очистке: ${e.message}", e)
            }
        }
    }
}