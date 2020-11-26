package jsonclass

import java.util.Date

case class OutputCsv(
  id: String,
  title: String,
  source: String,
  path: String,
  content: String,
  appType: String,
  createdDate: Date,
  lastModifiedDate: Date,
  version: Integer,
  latest: Boolean,
  roles: Array[String],
  metadata: Metadata,
  docFormat: String,
  sensitive: String,
  visualization: String,
  blurb: String,
  mailto: String,
  alternativeTitle: String,
  nlpDate: Date,
  connectorId: Integer,
  highlight: String,
  tags: Tag,
  annotations: String,
  subsite: String
) extends Serializable{

}
