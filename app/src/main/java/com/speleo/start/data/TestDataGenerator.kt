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

                    // 2. КОНТРОЛЬНЫЕ ПУНКТЫ
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

                    // 3. ПЕРСОНЫ
                    val maleFirst = listOf("Александр","Дмитрий","Максим","Сергей","Андрей","Алексей","Иван","Пётр","Николай","Владимир")
                    val femaleFirst = listOf("Анна","Мария","Елена","Ольга","Татьяна","Наталья","Екатерина","Ирина","Светлана","Юлия")
                    val maleLast = listOf("Иванов","Петров","Сидоров","Смирнов","Кузнецов","Попов","Васильев","Михайлов","Новиков","Фёдоров")
                    val femaleLast = listOf("Иванова","Петрова","Сидорова","Смирнова","Кузнецова","Попова","Васильева","Михайлова","Новикова","Фёдорова")
                    val nicks = listOf("Геолог","Спелеолог","Фонарик","Компас","Штольня","Каска","Верёвка","Камень","ЛетучаяМышь","Подземка","Шахтёр","Рудокоп","Сталкер","Крот","Бункер","Каньон","Сапёр","Маяк","Азимут","Горизонт")

                    val personIds = mutableListOf<Long>()
                    for (i in 0..19) {
                        val isMale = i < 10
                        val fn = if (isMale) maleFirst[i] else femaleFirst[i % 10]
                        val ln = if (isMale) maleLast[i] else femaleLast[i % 10]
                        val by = 2000 + (-20 + random.nextInt(32))
                        val bm = 1 + random.nextInt(12)
                        val bd = 1 + random.nextInt(28)
                        val birth = "%02d.%02d.%04d".format(bd, bm, by)
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

                    // 4. МЕНТОРЫ (первые 5 персон)
                    for (i in 0..4) {
                        if (i < personIds.size) {
                            mentorDao.insert(MentorEntity(personId = personIds[i]))
                        }
                    }
                    Log.d("TestDataGenerator", "✅ Создано 5 менторов")

                    // 5. КОМАНДЫ 2-го класса
                    for (t in 1..6) {
                        val tid = teamDao.insert(
                            TeamEntity(
                                competitionId = compId,
                                teamNumber = t,
                                className = "2",
                                status = when(t){
                                    1,2 -> "registered"
                                    3,4 -> "started"
                                    5 -> "finished"
                                    6 -> "disqualified"
                                    else -> "registered"
                                },
                                colorMark = "green"
                            )
                        )
                        if (tid == -1L) continue

                        val a = (t * 2) % personIds.size
                        val b = (t * 2 + 1) % personIds.size
                        // ИСПРАВЛЕНО: statusMember -> status
                        participantDao.insert(ParticipantEntity(teamId = tid, personId = personIds[a], role = "captain", status = "active"))
                        participantDao.insert(ParticipantEntity(teamId = tid, personId = personIds[b], role = "member", status = "active"))
                        if (t % 2 == 1 && t * 2 + 2 < personIds.size) {
                            participantDao.insert(ParticipantEntity(teamId = tid, personId = personIds[(t * 2 + 2) % personIds.size], role = "member", status = "active"))
                        }
                    }
                    Log.d("TestDataGenerator", "✅ Создано 6 команд 2-го класса")

                    // 6. КОМАНДЫ 3-го класса
                    for (t in 1..4) {
                        val tid = teamDao.insert(
                            TeamEntity(
                                competitionId = compId,
                                teamNumber = t,
                                className = "3",
                                status = when(t){
                                    1 -> "registered"
                                    2 -> "started"
                                    3 -> "finished"
                                    4 -> "lost"
                                    else -> "registered"
                                },
                                colorMark = "green"
                            )
                        )
                        if (tid == -1L) continue

                        val a = (t * 2 + 10) % personIds.size
                        val b = (t * 2 + 11) % personIds.size
                        // ИСПРАВЛЕНО: statusMember -> status
                        participantDao.insert(ParticipantEntity(teamId = tid, personId = personIds[a], role = "captain", status = "active"))
                        participantDao.insert(ParticipantEntity(teamId = tid, personId = personIds[b], role = "member", status = "active"))
                    }
                    Log.d("TestDataGenerator", "✅ Создано 4 команды 3-го класса")

                    Log.d("TestDataGenerator", "🎉 Тестовые данные успешно сгенерированы!")
                } catch (e: Exception) {
                    Log.e("TestDataGenerator", "❌ Ошибка при генерации: ${e.message}", e)
                }
            }
        }
    }

    suspend fun clearAll() {
        withContext(Dispatchers.IO) {
            try {
                // Очищаем настройки таймера
                appSettingsDao.remove("competition_start_timestamp")
                appSettingsDao.remove("competition_active")

                // Удаляем в правильном порядке (дети → родители)
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