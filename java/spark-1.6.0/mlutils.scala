package sparklyr

import org.apache.spark.ml._
import scala.util.{Try, Success, Failure}
import org.apache.spark.ml.param._
import org.apache.spark.ml.tuning.CrossValidator

object MLUtils {
  def createPipelineFromStages(uid: String, stages: PipelineStage*): Pipeline = {
    new Pipeline(uid)
      .setStages(stages.toArray)
  }

  def wrapInPipeline(pipelineStage: PipelineStage): Pipeline = {
    if (pipelineStage.isInstanceOf[Pipeline]) {
      pipelineStage.asInstanceOf[Pipeline]
    } else {
      new Pipeline()
      .setStages(Array(pipelineStage))
    }
  }

  def getParamMap(pipelineStage: PipelineStage): Map[String, Any] = {
    Map(pipelineStage.extractParamMap.toSeq map {
      pair => pair.param.name -> pair.value}: _*)
  }

  def composeStages(pipeline: Pipeline, stages: PipelineStage*): Pipeline = {
    new Pipeline()
    .setStages(explodePipeline(pipeline) ++ stages.flatMap(explodePipeline))
  }

  def getStages(stage: PipelineStage): Array[_ <: PipelineStage] = {
    if (stage.isInstanceOf[PipelineModel]) {
      stage.asInstanceOf[PipelineModel].stages
    } else {
      stage.asInstanceOf[Pipeline].getStages
    }
  }

  def explodePipeline(pipeline: PipelineStage): Array[PipelineStage] = {
    def f(stage: PipelineStage): Array[PipelineStage] = {
      val pipeline = Try(getStages(stage))
      pipeline match {
        case Failure(s) => Array(stage)
        case Success(s) => s.flatMap(f)
      }
    }
    f(pipeline) filterNot {_.isInstanceOf[Pipeline]}
  }

  def uidStagesMapping(pipeline: Pipeline): Map[String, PipelineStage] = {
    explodePipeline(pipeline).toSeq.map(x => (x.uid -> x)).toMap
  }

  def paramMapToList(paramMap: ParamMap): Map[String, Any] = {
    paramMap.toSeq.map(
      pair => (pair.param.name -> pair.value)
      ).toMap
  }

  def paramMapToTriples(paramMap: ParamMap): Array[(String, String, Any)] = {
    paramMap.toSeq.map(
      pair => (pair.param.parent, pair.param.name, pair.value)
      ).toArray
  }

  def setParamMaps(cv: CrossValidator, paramMaps: ParamMap*): CrossValidator = {
    cv.setEstimatorParamMaps(paramMaps.toArray)
  }
}
