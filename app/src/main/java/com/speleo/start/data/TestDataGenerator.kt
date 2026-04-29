package com.speleo.start.data

import android.util.Log
import androidx.room.withTransaction
import com.speleo.start.data.local.AppDatabase
import com.speleo.start.data.local.dao.*
import com.speleo.start.data.local.entity.*
import kotlinx.coroutines.*
import java.util.*
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
                val compId = competitionDao.insert(
                    CompetitionEntity(
                        name = "Бяковские каменоломни 2026",
                        shortName = "Бяки 2026",
                        date = "13.12.2025",
                        place = "Тульская обл., пос. Метростроевский",
                        discipline = "underground",
                        system = "Бяковская (Бяки)"
                    )
                )
                if (compId == -1L) {
                    Log.e("GEN", "FAILED competition insert")
                    return@withTransaction
                }

                for (i in 1..12) {
                    val cp = MasterRouteCardEntity(
                        competitionId = compId, displayNumber = i,
                        weight = 2 + random.nextInt(14),
                        type = if (i % 4 == 0) "technical" else "normal",
                        sortOrder = i,
                        normativeSeconds = if (i % 4 == 0) 60 + random.nextInt(241) else 0,
                        forClass2 = true, forClass3 = i <= 10,
                        trackWaitTime = i % 4 == 0, trackExecutionTime = false,
                        bonusPoints = 0
                    )
                    masterRouteCardDao.insert(cp)
                }

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
                    val age = 2025 - by
                    val pid = personDao.insert(PersonEntity(
                        lastName = ln, firstName = fn, middleName = if(random.nextBoolean()) "${fn}ович" else null,
                        nickname = nicks[i], birthDate = birth, phone = phone,
                        gender = if(isMale) "male" else "female"
                    ))
                    if (pid > 0) personIds.add(pid)
                }

                for (i in 0..4)
                    if (i < personIds.size) mentorDao.insert(MentorEntity(personId = personIds[i]))

                for (t in 1..6) {
                    val tid = teamDao.insert(TeamEntity(competitionId = compId, teamNumber = t, className = "2",
                        status = when(t){1,2->"registered";3,4->"started";5->"finished";6->"disqualified"; else->"registered"}, colorMark = "green"))
                    val a=(t*2)%personIds.size; val b=(t*2+1)%personIds.size
                    participantDao.insert(ParticipantEntity(teamId=tid, personId=personIds[a], role="captain", statusMember="active"))
                    participantDao.insert(ParticipantEntity(teamId=tid, personId=personIds[b], role="member", statusMember="active"))
                    if(t%2==1) participantDao.insert(ParticipantEntity(teamId=tid, personId=personIds[(t*2+2)%personIds.size], role="member", statusMember="active"))
                }

                for (t in 1..4) {
                    val tid = teamDao.insert(TeamEntity(competitionId = compId, teamNumber = t, className = "3",
                        status = when(t){1->"registered";2->"started";3->"finished";4->"lost"; else->"registered"}, colorMark = "green"))
                    val a=(t*2+10)%personIds.size; val b=(t*2+11)%personIds.size
                    participantDao.insert(ParticipantEntity(teamId=tid, personId=personIds[a], role="captain", statusMember="active"))
                    participantDao.insert(ParticipantEntity(teamId=tid, personId=personIds[b], role="member", statusMember="active"))
                }
            }
        }
    }

    suspend fun clearAll() {
        withContext(Dispatchers.IO) {
            appSettingsDao.remove("competition_start_timestamp")
            appSettingsDao.remove("competition_active")

            participantDao.deleteAll()
            mentorDao.deleteAll()
            masterRouteCardDao.deleteAll()

            teamDao.deleteAll()
            personDao.deleteAll()
            competitionDao.deleteAll()
        }
    }
}